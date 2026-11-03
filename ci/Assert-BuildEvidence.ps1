param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Validate', 'Fixtures')]
    [string] $Mode,

    [string] $ArtifactPath,
    [string] $SbomPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Fail-Evidence([string] $Message) {
    throw "Build evidence violation: $Message"
}

function Assert-Condition([bool] $Condition, [string] $Message) {
    if (-not $Condition) {
        Fail-Evidence $Message
    }
}

function Test-BuildEvidence([string] $JarPath, [string] $CycloneDxPath) {
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($JarPath)) 'JAR path is required'
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($CycloneDxPath)) 'SBOM path is required'
    Assert-Condition (Test-Path -LiteralPath $JarPath -PathType Leaf) "required JAR is missing: $JarPath"
    Assert-Condition (Test-Path -LiteralPath $CycloneDxPath -PathType Leaf) "required SBOM is missing: $CycloneDxPath"

    $jar = Get-Item -LiteralPath $JarPath
    $sbom = Get-Item -LiteralPath $CycloneDxPath
    Assert-Condition ($jar.Length -gt 0) "required JAR is empty: $JarPath"
    Assert-Condition ($sbom.Length -gt 0) "required SBOM is empty: $CycloneDxPath"

    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
        try {
            Assert-Condition ($archive.Entries.Count -gt 0) "required JAR archive contains no entries: $JarPath"
            Assert-Condition ($null -ne $archive.GetEntry('META-INF/MANIFEST.MF')) `
                "required JAR archive has no manifest: $JarPath"
        } finally {
            if ($null -ne $archive) {
                $archive.Dispose()
            }
        }
    } catch {
        if ($_.Exception.Message.StartsWith('Build evidence violation:')) {
            throw
        }
        Fail-Evidence "required JAR is not a valid archive: $JarPath"
    }

    try {
        $document = Get-Content -Raw -LiteralPath $sbom.FullName | ConvertFrom-Json -Depth 100
    } catch {
        Fail-Evidence "required SBOM is not valid JSON: $CycloneDxPath"
    }
    Assert-Condition ($document.bomFormat -eq 'CycloneDX') "required SBOM is not CycloneDX: $CycloneDxPath"
    Assert-Condition (@($document.components).Count -gt 0) "required SBOM contains no components: $CycloneDxPath"

    Write-Output "Build evidence validation passed: JAR=$($jar.FullName); SBOM=$($sbom.FullName)"
}

function Assert-Fails([scriptblock] $Action, [string] $Expected, [string] $Fixture) {
    try {
        & $Action
        Fail-Evidence "$Fixture unexpectedly passed"
    } catch {
        if (-not $_.Exception.Message.Contains($Expected)) {
            throw
        }
        Write-Output "${Fixture}: detected expected failure containing '$Expected'"
    }
}

function Invoke-BuildEvidenceFixtures {
    $fixtureRoot = Join-Path ([System.IO.Path]::GetTempPath()) `
        "neshane-v3-016-build-evidence-$([Guid]::NewGuid().ToString('N'))"
    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null

    try {
        $validJar = Join-Path $fixtureRoot 'reference-service.jar'
        $validSbom = Join-Path $fixtureRoot 'bom.json'
        $emptyJar = Join-Path $fixtureRoot 'empty.jar'
        $emptySbom = Join-Path $fixtureRoot 'empty.json'
        $missingJar = Join-Path $fixtureRoot 'missing.jar'
        $missingSbom = Join-Path $fixtureRoot 'missing.json'

        $staging = Join-Path $fixtureRoot 'jar-content'
        $manifestDirectory = Join-Path $staging 'META-INF'
        New-Item -ItemType Directory -Path $manifestDirectory | Out-Null
        Set-Content -LiteralPath (Join-Path $manifestDirectory 'MANIFEST.MF') `
            -Value "Manifest-Version: 1.0`n" -Encoding utf8NoBOM
        [System.IO.Compression.ZipFile]::CreateFromDirectory($staging, $validJar)

        @{
            bomFormat = 'CycloneDX'
            specVersion = '1.6'
            components = @(@{ type = 'application'; name = 'reference-service'; version = 'fixture' })
        } | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $validSbom -Encoding utf8NoBOM
        [System.IO.File]::WriteAllBytes($emptyJar, [byte[]]::new(0))
        [System.IO.File]::WriteAllBytes($emptySbom, [byte[]]::new(0))

        Assert-Fails { Test-BuildEvidence $missingJar $validSbom } `
            'required JAR is missing' 'Missing JAR fixture'
        Assert-Fails { Test-BuildEvidence $validJar $missingSbom } `
            'required SBOM is missing' 'Missing SBOM fixture'
        Assert-Fails { Test-BuildEvidence $emptyJar $validSbom } `
            'required JAR is empty' 'Empty JAR fixture'
        Assert-Fails { Test-BuildEvidence $validJar $emptySbom } `
            'required SBOM is empty' 'Empty SBOM fixture'
        Assert-Fails { Test-BuildEvidence (Join-Path $fixtureRoot 'wrong/path/reference-service.jar') $validSbom } `
            'required JAR is missing' 'Incorrect artifact path fixture'
        Test-BuildEvidence $validJar $validSbom
        Write-Output 'Valid JAR and SBOM fixture: PASS'
    } finally {
        if (Test-Path -LiteralPath $fixtureRoot) {
            Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
        }
    }
}

switch ($Mode) {
    'Validate' {
        Test-BuildEvidence $ArtifactPath $SbomPath
    }
    'Fixtures' {
        Invoke-BuildEvidenceFixtures
        Write-Output 'All V3-016 build-evidence negative and corrected fixtures passed.'
    }
}
