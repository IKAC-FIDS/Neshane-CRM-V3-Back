$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Test-Ignored {
    param([string]$Path)

    & git -C $root check-ignore --no-index --quiet -- $Path
    $code = $LASTEXITCODE

    if ($code -eq 0) { return $true }
    if ($code -eq 1) { return $false }

    throw "git check-ignore failed for: $Path"
}

$sourcePaths = @(
    'services/reference-service/src/main/java/ir/neshane/reference/application/port/out/ReferencePeerPort.java',
    'services/example/src/main/java/ir/neshane/example/application/port/out/ExamplePort.java'
)

foreach ($path in $sourcePaths) {
    if (Test-Ignored $path) {
        throw "Source path must not be ignored: $path"
    }
}

$buildPaths = @(
    'out/example.class',
    'build/example.class',
    'services/reference-service/build/example.class'
)

foreach ($path in $buildPaths) {
    if (-not (Test-Ignored $path)) {
        throw "Build output must be ignored: $path"
    }
}

$trackedPort = $sourcePaths[0]

& git -C $root ls-files --error-unmatch -- $trackedPort *> $null

if ($LASTEXITCODE -ne 0) {
    throw "Required application port is not tracked: $trackedPort"
}

$ignored = @(
    & git -C $root ls-files --others --ignored --exclude-standard -z
)

if ($LASTEXITCODE -ne 0) {
    throw 'Could not enumerate ignored repository files'
}

$ignoredJava = @(
    ($ignored -join '') -split "`0" |
        Where-Object { $_ -match '(?i)\.java$' }
)

if ($ignoredJava.Count -gt 0) {
    throw "Ignored Java source files detected: $($ignoredJava -join ', ')"
}

Write-Output 'SOURCE_IGNORE_POLICY=PASS'
