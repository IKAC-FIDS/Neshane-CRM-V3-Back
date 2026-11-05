param(
    [switch]$SkipDocker
)

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$java = (Get-Command java).Source
$primaryJar = Join-Path $backendRoot 'services\reference-service\build\libs\reference-service.jar'
$peerJar = Join-Path $backendRoot 'services\reference-peer-service\build\libs\reference-peer-service.jar'
$evidenceDir = Join-Path $backendRoot 'build\v3-017-smoke'
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

function Start-ServiceProcess {
    param([string]$Jar, [int]$Port, [hashtable]$ExtraEnvironment, [string]$Name)
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $java
    $info.ArgumentList.Add('-jar')
    $info.ArgumentList.Add($Jar)
    $info.UseShellExecute = $false
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.Environment['SERVER_PORT'] = $Port.ToString()
    $info.Environment['NESHANE_DEPLOYMENT_PROFILE'] = 'development'
    foreach ($entry in $ExtraEnvironment.GetEnumerator()) { $info.Environment[$entry.Key] = $entry.Value }
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $info
    [void]$process.Start()
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()
    return $process
}

function Wait-Healthy([int]$Port) {
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 1
            if ($response.StatusCode -eq 200) { return }
        } catch { Start-Sleep -Milliseconds 250 }
    }
    throw "Service on port $Port did not become healthy"
}

function Stop-OwnedProcess($Process) {
    if ($null -ne $Process -and -not $Process.HasExited) {
        $Process.Kill($true)
        $Process.WaitForExit(5000) | Out-Null
    }
}

$primary = $null
$peer = $null
$network = 'neshane-v3-017-smoke'
$containers = @('neshane-v3-017-primary', 'neshane-v3-017-peer')
try {
    $peer = Start-ServiceProcess $peerJar 28081 @{} 'peer'
    $primary = Start-ServiceProcess $primaryJar 28080 @{ REFERENCE_PEER_BASE_URL = 'http://127.0.0.1:28081' } 'primary'
    Wait-Healthy 28081
    Wait-Healthy 28080

    $headers = @{
        'X-Correlation-ID' = '123e4567-e89b-12d3-a456-426614174000'
        traceparent = '00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01'
    }
    $body = '{"message":"process-smoke"}'
    $response = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:28080/api/v1/reference-relays' `
        -Headers $headers -ContentType 'application/json' -Body $body
    if ($response.responder -ne 'reference-peer-service' -or $response.traceId -ne '0af7651916cd43dd8448eb211c80319c') {
        throw 'Process interaction did not preserve the expected responder and trace.'
    }

    Stop-OwnedProcess $peer
    $peer = $null
    $failure = Invoke-WebRequest -Method Post -Uri 'http://127.0.0.1:28080/api/v1/reference-relays' `
        -ContentType 'application/json' -Body $body -SkipHttpErrorCheck
    $failureBody = if ($failure.Content -is [byte[]]) {
        [System.Text.Encoding]::UTF8.GetString($failure.Content)
    } else { [string]$failure.Content }
    if ($failure.StatusCode -ne 503 -or $failureBody -notmatch 'REFERENCE_PEER_UNAVAILABLE') {
        throw "Primary service did not fail safely while peer was unavailable: status=$($failure.StatusCode); body=$failureBody"
    }

    $peer = Start-ServiceProcess $peerJar 28081 @{} 'peer-restarted'
    Wait-Healthy 28081
    $recovered = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:28080/api/v1/reference-relays' `
        -Headers $headers -ContentType 'application/json' -Body $body
    if ($recovered.responder -ne 'reference-peer-service') { throw 'Recovery after peer restart failed.' }

    Stop-OwnedProcess $primary
    $primary = $null
    $peerHealth = Invoke-WebRequest -Uri 'http://127.0.0.1:28081/actuator/health'
    if ($peerHealth.StatusCode -ne 200) { throw 'Peer did not remain independently available.' }
    Write-Output 'PROCESS_SMOKE=PASS'

    if (-not $SkipDocker) {
        docker build -t neshane/reference-service:v3-017-local (Join-Path $backendRoot 'services\reference-service') | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Reference Service image build failed.' }
        docker build -t neshane/reference-peer-service:v3-017-local (Join-Path $backendRoot 'services\reference-peer-service') | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Reference Peer image build failed.' }
        docker network create $network | Out-Null
        docker run -d --name $containers[1] --network $network --read-only --cap-drop ALL `
            --security-opt no-new-privileges --tmpfs /tmp:rw,noexec,nosuid,size=64m `
            --memory 512m --cpus 1 -p 127.0.0.1:28083:8080 `
            -e NESHANE_DEPLOYMENT_PROFILE=development neshane/reference-peer-service:v3-017-local | Out-Null
        docker run -d --name $containers[0] --network $network --read-only --cap-drop ALL `
            --security-opt no-new-privileges --tmpfs /tmp:rw,noexec,nosuid,size=64m `
            --memory 512m --cpus 1 -p 127.0.0.1:28082:8080 `
            -e NESHANE_DEPLOYMENT_PROFILE=development `
            -e REFERENCE_PEER_BASE_URL=http://neshane-v3-017-peer:8080 `
            neshane/reference-service:v3-017-local | Out-Null
        Wait-Healthy 28083
        Wait-Healthy 28082
        $containerResponse = Invoke-RestMethod -Method Post `
            -Uri 'http://127.0.0.1:28082/api/v1/reference-relays' -Headers $headers `
            -ContentType 'application/json' -Body '{"message":"container-smoke"}'
        if ($containerResponse.responder -ne 'reference-peer-service') { throw 'Container interaction failed.' }
        Write-Output 'CONTAINER_SMOKE=PASS'
    }
} finally {
    Stop-OwnedProcess $primary
    Stop-OwnedProcess $peer
    if (-not $SkipDocker) {
        foreach ($container in $containers) { docker rm -f $container 2>$null | Out-Null }
        docker network rm $network 2>$null | Out-Null
    }
}
