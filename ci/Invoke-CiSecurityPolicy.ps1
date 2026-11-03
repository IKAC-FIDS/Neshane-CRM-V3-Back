param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('ScanSecrets', 'Workflow', 'Exceptions', 'Fixtures', 'Sbom', 'Vulnerability', 'Evidence')]
    [string] $Mode,

    [Parameter(Mandatory = $true)]
    [string] $RepositoryRoot,

    [string] $InputPath,
    [string] $SecondaryPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repository = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$policyPath = Join-Path $repository 'ci/security-policy.json'
$exceptionsPath = Join-Path $repository 'ci/security-exceptions.json'

function Fail-Policy([string] $Message) {
    throw "CI security policy violation: $Message"
}

function Read-JsonFile([string] $Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Fail-Policy "required JSON file is missing: $Path"
    }
    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json -Depth 100
    } catch {
        Fail-Policy "invalid JSON in ${Path}: $($_.Exception.Message)"
    }
}

function Assert-Condition([bool] $Condition, [string] $Message) {
    if (-not $Condition) {
        Fail-Policy $Message
    }
}

function Get-ObjectProperty($Object, [string] $Name) {
    if ($null -eq $Object) {
        return $null
    }
    return $Object.PSObject.Properties[$Name]
}

$policy = Read-JsonFile $policyPath
$exceptionDocument = Read-JsonFile $exceptionsPath

function Test-ExceptionDocument($Document, $Policy, [DateTimeOffset] $Now) {
    Assert-Condition ($Document.schemaVersion -eq 1) 'security exception schemaVersion must be 1'
    Assert-Condition ($null -ne $Document.exceptions) 'security exception list is missing'

    $ids = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($exception in @($Document.exceptions)) {
        foreach ($field in @($Policy.exceptions.requiredFields)) {
            $property = $exception.PSObject.Properties[$field]
            Assert-Condition ($null -ne $property -and -not [string]::IsNullOrWhiteSpace([string] $property.Value)) `
                "exception is missing required field: $field"
        }

        Assert-Condition ($ids.Add([string] $exception.id)) "duplicate exception id: $($exception.id)"
        Assert-Condition (-not (@($Policy.exceptions.nonWaivableControls) -contains [string] $exception.control)) `
            "control is non-waivable: $($exception.control)"

        $created = [DateTimeOffset]::Parse([string] $exception.createdAt)
        $expires = [DateTimeOffset]::Parse([string] $exception.expiresAt)
        Assert-Condition ($expires -gt $Now) "exception is expired: $($exception.id)"
        Assert-Condition ($expires -gt $created) "exception expiry is not after creation: $($exception.id)"
        Assert-Condition (($expires - $created).TotalDays -le [double] $Policy.exceptions.maximumLifetimeDays) `
            "exception lifetime exceeds policy maximum: $($exception.id)"
    }
}

function Get-SecretPatterns {
    return @(
        ('AKIA' + '[0-9A-Z]{16}'),
        ('gh' + '[pousr]_[A-Za-z0-9]{36,255}'),
        ('-----BEGIN ' + '(RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----')
    )
}

function Test-TextForSecrets([string] $Text, [string] $Source) {
    foreach ($pattern in Get-SecretPatterns) {
        if ($Text -match $pattern) {
            Fail-Policy "high-confidence secret material detected in $Source"
        }
    }
}

function Invoke-RepositorySecretScan {
    $allowedExtensions = @('.java', '.gradle', '.md', '.yml', '.yaml', '.json', '.properties', '.toml', '.ps1', '.xml', '.sh', '.bat')
    $files = Get-ChildItem -LiteralPath $repository -Recurse -File | Where-Object {
        $relative = $_.FullName.Substring($repository.Length).TrimStart([char[]]@('/', '\'))
        $segments = $relative -split '[/\\]'
        -not ($segments -contains '.git') -and
        -not ($segments -contains '.gradle') -and
        -not ($segments -contains 'build') -and
        ($allowedExtensions -contains $_.Extension.ToLowerInvariant() -or $_.Name -eq 'Dockerfile')
    }

    Assert-Condition ($files.Count -gt 0) 'secret scan scope is unexpectedly empty'
    foreach ($file in $files) {
        Test-TextForSecrets (Get-Content -Raw -LiteralPath $file.FullName) $file.FullName
    }
    Write-Output "Secret policy scan passed for $($files.Count) repository text files."
}

function Test-WorkflowPolicy {
    $ciWorkflow = Join-Path $repository '.github/workflows/backend-ci.yml'
    $promotionWorkflow = Join-Path $repository '.github/workflows/backend-promotion-evidence.yml'
    $workflowPaths = @($ciWorkflow, $promotionWorkflow)

    foreach ($workflow in $workflowPaths) {
        Assert-Condition (Test-Path -LiteralPath $workflow -PathType Leaf) "workflow is missing: $workflow"
        $content = Get-Content -Raw -LiteralPath $workflow
        Assert-Condition ($content -match '(?m)^permissions:\s*\r?\n\s+contents:\s+read\s*$') `
            "workflow must default to contents: read: $workflow"
        Assert-Condition ($content -notmatch '(?im)^\s*continue-on-error:\s*true\s*$') `
            "continue-on-error true is prohibited: $workflow"
        Assert-Condition ($content -notmatch '(?m)^\s*pull_request_target\s*:') `
            "pull_request_target is prohibited: $workflow"
        Assert-Condition ($content -notmatch '(?im)^\s*permissions:\s*write-all\s*$') `
            "write-all permissions are prohibited: $workflow"
        Assert-Condition ($content -match 'persist-credentials:\s*false') `
            "checkout credentials must not persist: $workflow"

        $usesLines = [regex]::Matches($content, '(?m)^\s*-?\s*uses:\s*([^\s#]+)')
        Assert-Condition ($usesLines.Count -gt 0) "workflow has no actions: $workflow"
        foreach ($match in $usesLines) {
            $reference = $match.Groups[1].Value
            Assert-Condition ($reference -match '@[0-9a-f]{40}$') `
                "action is not pinned to an immutable commit: $reference"
        }
    }

    $ciContent = Get-Content -Raw -LiteralPath $ciWorkflow
    Assert-Condition ($ciContent -match '(?m)^\s*pull_request:\s*$') `
        'PR workflow must use the unprivileged pull_request trigger'
    Assert-Condition ($ciContent -notmatch '(?im)^\s*(id-token|attestations|packages|actions):\s*write\s*$') `
        'PR workflow must not grant privileged write permissions'
    foreach ($marker in @('engineering-gates:', 'secret-scan:', 'vulnerability-scan:', 'evidence-gate:',
            'ciLocalVerification', 'gitleaks-action', 'trivy-action', 'cyclonedx')) {
        Assert-Condition ($ciContent.Contains($marker)) "PR workflow is missing mandatory marker: $marker"
    }
    Assert-Condition ([regex]::Matches($ciContent, "cache:\s*'false'").Count -ge 1) `
        'PR vulnerability scan must disable the reusable scanner cache'

    $promotionContent = Get-Content -Raw -LiteralPath $promotionWorkflow
    foreach ($marker in @('workflow_dispatch:', 'container-scan', 'attest-build-provenance',
            'gh attestation verify', 'verifyPromotionEvidence', 'trivy-action')) {
        Assert-Condition ($promotionContent.Contains($marker)) "promotion workflow is missing mandatory marker: $marker"
    }
    Assert-Condition ($promotionContent -match "if:\s*github\.ref\s*==\s*'refs/heads/main'") `
        'promotion evidence must be restricted to the main branch'
    Assert-Condition ([regex]::Matches($promotionContent, "cache:\s*'false'").Count -ge 2) `
        'promotion filesystem and container scans must disable the reusable scanner cache'

    Write-Output 'Workflow policy validation passed with immutable action pins and mandatory fail-closed stages.'
}

function Test-SbomDocument($Document) {
    Assert-Condition ($Document.bomFormat -eq 'CycloneDX') 'SBOM bomFormat must be CycloneDX'
    Assert-Condition (-not [string]::IsNullOrWhiteSpace([string] $Document.specVersion)) 'SBOM specVersion is missing'
    Assert-Condition ([string] $Document.serialNumber -match '^urn:uuid:[0-9a-fA-F-]{36}$') 'SBOM serialNumber is invalid'
    Assert-Condition ($null -ne $Document.metadata -and $null -ne $Document.metadata.component) `
        'SBOM metadata.component is missing'
    Assert-Condition (@($Document.components).Count -gt 0) 'SBOM contains no dependency components'
    Assert-Condition ($null -ne $Document.dependencies) 'SBOM dependency graph is missing'
}

function Get-ActiveException($Exceptions, [string] $FindingId, [DateTimeOffset] $Now) {
    foreach ($exception in @($Exceptions.exceptions)) {
        if ($exception.control -eq 'VULNERABILITY_FINDING' -and
            $exception.findingId -eq $FindingId -and
            [DateTimeOffset]::Parse([string] $exception.expiresAt) -gt $Now) {
            return $exception
        }
    }
    return $null
}

function Test-VulnerabilityDocument($Report, $KevDocument, $Policy, $Exceptions, [DateTimeOffset] $Now) {
    Assert-Condition ($null -ne $Report) 'scanner report is missing'
    $schemaVersionProperty = Get-ObjectProperty $Report 'SchemaVersion'
    $artifactNameProperty = Get-ObjectProperty $Report 'ArtifactName'
    $artifactTypeProperty = Get-ObjectProperty $Report 'ArtifactType'
    $resultsProperty = Get-ObjectProperty $Report 'Results'
    Assert-Condition ($null -ne $schemaVersionProperty -and $schemaVersionProperty.Value -is [ValueType] -and
        [int64] $schemaVersionProperty.Value -gt 0) 'scanner report SchemaVersion is missing or invalid'
    Assert-Condition ($null -ne $artifactNameProperty -and
        -not [string]::IsNullOrWhiteSpace([string] $artifactNameProperty.Value)) `
        'scanner report ArtifactName is missing'
    Assert-Condition ($null -ne $artifactTypeProperty -and
        -not [string]::IsNullOrWhiteSpace([string] $artifactTypeProperty.Value)) `
        'scanner report ArtifactType is missing'
    Assert-Condition ($null -ne $resultsProperty) 'scanner report Results are missing'
    Assert-Condition (@($resultsProperty.Value).Count -gt 0) 'scanner report contains no analyzed targets'

    Assert-Condition ($null -ne $KevDocument) 'CISA KEV document is missing'
    $kevVulnerabilitiesProperty = Get-ObjectProperty $KevDocument 'vulnerabilities'
    Assert-Condition ($null -ne $kevVulnerabilitiesProperty) 'CISA KEV vulnerability list is missing'

    $kevIds = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($entry in @($kevVulnerabilitiesProperty.Value)) {
        $cveProperty = Get-ObjectProperty $entry 'cveID'
        Assert-Condition ($null -ne $cveProperty -and
            -not [string]::IsNullOrWhiteSpace([string] $cveProperty.Value)) `
            'CISA KEV entry cveID is missing'
        [void] $kevIds.Add([string] $cveProperty.Value)
    }

    $blocked = @()
    foreach ($result in @($resultsProperty.Value)) {
        $targetProperty = Get-ObjectProperty $result 'Target'
        Assert-Condition ($null -ne $targetProperty -and
            -not [string]::IsNullOrWhiteSpace([string] $targetProperty.Value)) `
            'scanner result target is missing'
        $vulnerabilitiesProperty = Get-ObjectProperty $result 'Vulnerabilities'
        if ($null -eq $vulnerabilitiesProperty -or $null -eq $vulnerabilitiesProperty.Value) {
            continue
        }
        foreach ($finding in @($vulnerabilitiesProperty.Value)) {
            $findingIdProperty = Get-ObjectProperty $finding 'VulnerabilityID'
            $severityProperty = Get-ObjectProperty $finding 'Severity'
            Assert-Condition ($null -ne $findingIdProperty -and
                -not [string]::IsNullOrWhiteSpace([string] $findingIdProperty.Value)) `
                "scanner vulnerability in target '$($targetProperty.Value)' has no VulnerabilityID"
            Assert-Condition ($null -ne $severityProperty -and
                -not [string]::IsNullOrWhiteSpace([string] $severityProperty.Value)) `
                "scanner vulnerability '$($findingIdProperty.Value)' has no Severity"
            $findingId = [string] $findingIdProperty.Value
            $severity = ([string] $severityProperty.Value).ToUpperInvariant()
            Assert-Condition (@('UNKNOWN', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL') -contains $severity) `
                "scanner vulnerability '$findingId' has unsupported Severity: $severity"
            if ($kevIds.Contains($findingId)) {
                $blocked += "$findingId is listed in CISA KEV"
                continue
            }
            if (@($Policy.vulnerability.blockedSeverities) -contains $severity) {
                if ($null -eq (Get-ActiveException $Exceptions $findingId $Now)) {
                    $blocked += "$findingId has blocked severity $severity"
                }
            }
        }
    }
    Assert-Condition ($blocked.Count -eq 0) ("blocked vulnerabilities: " + ($blocked -join '; '))
}

function Test-EvidenceDocument($Document, $Policy) {
    Assert-Condition (@('PULL_REQUEST', 'PROMOTION') -contains [string] $Document.kind) `
        'evidence kind must be PULL_REQUEST or PROMOTION'
    Assert-Condition ([string] $Document.commitSha -match '^[0-9a-f]{40}$') 'evidence commitSha is invalid'
    Assert-Condition ([string] $Document.artifactSha256 -match '^[0-9a-f]{64}$') 'artifact SHA-256 is invalid'
    Assert-Condition ([string] $Document.sbomSha256 -match '^[0-9a-f]{64}$') 'SBOM SHA-256 is invalid'
    Assert-Condition ($null -ne $Document.stages) 'evidence stages are missing'

    $required = if ($Document.kind -eq 'PROMOTION') {
        @($Policy.evidence.promotionRequiredStages)
    } else {
        @($Policy.evidence.pullRequestRequiredStages)
    }
    foreach ($stage in $required) {
        $property = $Document.stages.PSObject.Properties[$stage]
        Assert-Condition ($null -ne $property) "mandatory evidence stage is missing: $stage"
        Assert-Condition ($property.Value -eq 'PASS') "mandatory evidence stage did not pass: $stage"
    }
    if ($Document.kind -eq 'PROMOTION') {
        Assert-Condition ($Document.provenanceVerified -eq $true) 'promotion provenance was not verified'
    }
}

function Assert-Fails([scriptblock] $Action, [string] $Expected, [string] $Fixture) {
    try {
        & $Action
        Fail-Policy "$Fixture unexpectedly passed"
    } catch {
        if (-not $_.Exception.Message.Contains($Expected)) {
            throw
        }
        Write-Output "${Fixture}: detected expected failure containing '$Expected'"
    }
}

function Invoke-PolicyFixtures {
    $now = [DateTimeOffset]::UtcNow

    $syntheticSecret = 'AKIA' + ('A' * 16)
    Assert-Fails { Test-TextForSecrets "credential=$syntheticSecret" 'synthetic-secret-fixture' } `
        'high-confidence secret material' 'Secret exposure fixture'
    Test-TextForSecrets 'credential-reference=environment-variable-name' 'corrected-secret-fixture'
    Write-Output 'Secret exposure corrected fixture: PASS'

    $validSbom = [pscustomobject]@{
        bomFormat = 'CycloneDX'; specVersion = '1.6'; serialNumber = 'urn:uuid:11111111-1111-4111-8111-111111111111'
        metadata = [pscustomobject]@{ component = [pscustomobject]@{ name = 'fixture'; type = 'application' } }
        components = @([pscustomobject]@{ name = 'dependency'; version = '1.0.0' })
        dependencies = @([pscustomobject]@{ ref = 'fixture'; dependsOn = @('dependency') })
    }
    $invalidSbom = $validSbom.PSObject.Copy()
    $invalidSbom.components = @()
    Assert-Fails { Test-SbomDocument $invalidSbom } 'contains no dependency components' 'Invalid SBOM fixture'
    Test-SbomDocument $validSbom
    Write-Output 'Corrected SBOM fixture: PASS'

    $cleanKev = [pscustomobject]@{ vulnerabilities = @() }
    $vulnerableReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactName = '.'; ArtifactType = 'filesystem'
        Results = @([pscustomobject]@{ Target = 'synthetic-vulnerable-target'; Vulnerabilities = @([pscustomobject]@{
            VulnerabilityID = 'CVE-2099-0001'; Severity = 'CRITICAL'
        }) })
    }
    Assert-Fails { Test-VulnerabilityDocument $vulnerableReport $cleanKev $policy $exceptionDocument $now } `
        'blocked severity CRITICAL' 'Vulnerable dependency fixture'
    $missingTargetsReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactName = '.'; ArtifactType = 'filesystem'; Results = @()
    }
    Assert-Fails { Test-VulnerabilityDocument $missingTargetsReport $cleanKev $policy $exceptionDocument $now } `
        'contains no analyzed targets' 'Missing scanner targets fixture'
    $missingMetadataReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactType = 'filesystem'
        Results = @([pscustomobject]@{ Target = 'synthetic-target-with-missing-metadata' })
    }
    Assert-Fails { Test-VulnerabilityDocument $missingMetadataReport $cleanKev $policy $exceptionDocument $now } `
        'scanner report ArtifactName is missing' 'Missing scanner metadata fixture'
    $malformedFindingReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactName = '.'; ArtifactType = 'filesystem'
        Results = @([pscustomobject]@{ Target = 'synthetic-malformed-target'; Vulnerabilities = @([pscustomobject]@{
            Severity = 'LOW'
        }) })
    }
    Assert-Fails { Test-VulnerabilityDocument $malformedFindingReport $cleanKev $policy $exceptionDocument $now } `
        'has no VulnerabilityID' 'Malformed vulnerability record fixture'
    $absentVulnerabilitiesReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactName = '.'; ArtifactType = 'filesystem'
        Results = @([pscustomobject]@{ Target = 'synthetic-clean-target-with-absent-property' })
    }
    Test-VulnerabilityDocument $absentVulnerabilitiesReport $cleanKev $policy $exceptionDocument $now
    Write-Output 'Absent Vulnerabilities property fixture: PASS'
    $cleanReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactName = '.'; ArtifactType = 'filesystem'
        Results = @([pscustomobject]@{ Target = 'synthetic-clean-target'; Vulnerabilities = @() })
    }
    Test-VulnerabilityDocument $cleanReport $cleanKev $policy $exceptionDocument $now
    Write-Output 'Empty Vulnerabilities collection fixture: PASS'

    $kevReport = [pscustomobject]@{
        SchemaVersion = 2; ArtifactName = '.'; ArtifactType = 'filesystem'
        Results = @([pscustomobject]@{ Target = 'synthetic-kev-target'; Vulnerabilities = @([pscustomobject]@{
            VulnerabilityID = 'CVE-2099-0002'; Severity = 'LOW'
        }) })
    }
    $kev = [pscustomobject]@{ vulnerabilities = @([pscustomobject]@{ cveID = 'CVE-2099-0002' }) }
    Assert-Fails { Test-VulnerabilityDocument $kevReport $kev $policy $exceptionDocument $now } `
        'listed in CISA KEV' 'Known-exploited vulnerability fixture'

    $missingScannerEvidence = Join-Path ([System.IO.Path]::GetTempPath()) `
        "neshane-v3-016-missing-$([Guid]::NewGuid().ToString('N')).json"
    Assert-Fails { Read-JsonFile $missingScannerEvidence } `
        'required JSON file is missing' 'Missing scanner evidence fixture'

    $expired = [pscustomobject]@{
        schemaVersion = 1
        exceptions = @([pscustomobject]@{
            id = 'EX-TEST'; control = 'VULNERABILITY_FINDING'; findingId = 'CVE-2099-0003'; owner = 'fixture-owner'
            approvedBy = 'fixture-approver'; approvalReference = 'fixture-approval-reference'
            reason = 'fixture'; compensatingControl = 'fixture-control'
            createdAt = $now.AddDays(-2).ToString('o'); expiresAt = $now.AddDays(-1).ToString('o')
        })
    }
    Assert-Fails { Test-ExceptionDocument $expired $policy $now } 'exception is expired' 'Expired exception fixture'
    $missingApproval = [pscustomobject]@{
        schemaVersion = 1
        exceptions = @([pscustomobject]@{
            id = 'EX-NO-APPROVAL'; control = 'VULNERABILITY_FINDING'; findingId = 'CVE-2099-0004'
            owner = 'fixture-owner'; approvedBy = 'fixture-approver'; reason = 'fixture'
            compensatingControl = 'fixture-control'; createdAt = $now.ToString('o')
            expiresAt = $now.AddDays(1).ToString('o')
        })
    }
    Assert-Fails { Test-ExceptionDocument $missingApproval $policy $now } `
        'exception is missing required field: approvalReference' 'Unapproved exception fixture'
    Test-ExceptionDocument $exceptionDocument $policy $now
    Write-Output 'Corrected exception fixture: PASS'

    $hash = 'a' * 64
    $validEvidence = [pscustomobject]@{
        kind = 'PULL_REQUEST'; commitSha = 'b' * 40; artifactSha256 = $hash; sbomSha256 = $hash
        stages = [pscustomobject]@{ engineering = 'PASS'; secretScan = 'PASS'; vulnerabilityScan = 'PASS'; sbom = 'PASS' }
    }
    $missingEvidence = $validEvidence.PSObject.Copy()
    $missingEvidence.stages = [pscustomobject]@{ engineering = 'PASS'; secretScan = 'PASS'; sbom = 'PASS' }
    Assert-Fails { Test-EvidenceDocument $missingEvidence $policy } `
        'mandatory evidence stage is missing: vulnerabilityScan' 'Missing evidence fixture'
    Test-EvidenceDocument $validEvidence $policy
    Write-Output 'Corrected evidence fixture: PASS'

    $rsa = [System.Security.Cryptography.RSA]::Create(2048)
    try {
        $payload = [Text.Encoding]::UTF8.GetBytes('synthetic-provenance')
        $signature = $rsa.SignData(
            $payload,
            [System.Security.Cryptography.HashAlgorithmName]::SHA256,
            [System.Security.Cryptography.RSASignaturePadding]::Pss)
        Assert-Condition ($rsa.VerifyData($payload, $signature,
                [System.Security.Cryptography.HashAlgorithmName]::SHA256,
                [System.Security.Cryptography.RSASignaturePadding]::Pss)) 'valid synthetic signature did not verify'
        $signature[0] = $signature[0] -bxor 1
        Assert-Fails {
            Assert-Condition ($rsa.VerifyData($payload, $signature,
                    [System.Security.Cryptography.HashAlgorithmName]::SHA256,
                    [System.Security.Cryptography.RSASignaturePadding]::Pss)) 'invalid signature or provenance'
        } 'invalid signature or provenance' 'Invalid signature fixture'
        Write-Output 'Corrected signature fixture: PASS'
    } finally {
        $rsa.Dispose()
    }
}

switch ($Mode) {
    'ScanSecrets' {
        Invoke-RepositorySecretScan
    }
    'Workflow' {
        Test-WorkflowPolicy
    }
    'Exceptions' {
        Test-ExceptionDocument $exceptionDocument $policy ([DateTimeOffset]::UtcNow)
        Write-Output 'Security exception policy validation passed.'
    }
    'Fixtures' {
        Invoke-PolicyFixtures
        Write-Output 'All V3-016 negative and corrected supply-chain fixtures passed.'
    }
    'Sbom' {
        Test-SbomDocument (Read-JsonFile $InputPath)
        Write-Output "CycloneDX SBOM validation passed: $InputPath"
    }
    'Vulnerability' {
        Test-ExceptionDocument $exceptionDocument $policy ([DateTimeOffset]::UtcNow)
        Test-VulnerabilityDocument (Read-JsonFile $InputPath) (Read-JsonFile $SecondaryPath) `
            $policy $exceptionDocument ([DateTimeOffset]::UtcNow)
        Write-Output 'Vulnerability and CISA KEV policy validation passed.'
    }
    'Evidence' {
        Test-EvidenceDocument (Read-JsonFile $InputPath) $policy
        Write-Output "CI evidence validation passed: $InputPath"
    }
}
