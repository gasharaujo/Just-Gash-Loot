param([switch]$Offline, [switch]$Install)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Find-Jdk([int]$Major) {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($root in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft", "$env:USERPROFILE\.gradle\jdks")) {
        if (Test-Path -LiteralPath $root) { $candidates += Get-ChildItem -Directory -LiteralPath $root | Select-Object -ExpandProperty FullName }
    }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        $java = Join-Path $candidate 'bin/java.exe'
        $releaseFile = Join-Path $candidate 'release'
        $compiler = Join-Path $candidate 'bin/javac.exe'
        if ((Test-Path -LiteralPath $java) -and (Test-Path -LiteralPath $compiler) -and (Test-Path -LiteralPath $releaseFile)) {
            $version = Get-Content -Raw -LiteralPath $releaseFile
            if ($version -match ('JAVA_VERSION="' + $Major + '(\.|"|-)')) { return $candidate }
        }
    }
    throw "JDK $Major not found. Install it or configure JAVA_HOME."
}

$matrix = @(
    @{ Module = 'forge-1.20.1'; Java = 17; Instance = '20.1'; Jar = 'justgashloot-forge-1.20.1-2.0.0.jar' },
    @{ Module = 'neoforge-1.21.1'; Java = 21; Instance = '21'; Jar = 'justgashloot-neoforge-1.21.1-2.0.0.jar' },
    @{ Module = 'neoforge-26.1.2'; Java = 25; Instance = '26.1.2'; Jar = 'justgashloot-neoforge-26.1.2-2.0.0.jar' }
)
$originalJavaHome = $env:JAVA_HOME
try {
    $artifactDirectory = Join-Path $projectRoot 'artifacts'
    New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
    $env:JAVA_HOME = Find-Jdk 21
    & python (Join-Path $PSScriptRoot 'generate_resources.py')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar recursos' }
    foreach ($target in $matrix) {
        $env:JAVA_HOME = Find-Jdk $target.Java
        Push-Location (Join-Path $projectRoot $target.Module)
        try {
            $buildArguments = @('build', '--console=plain')
            if ($Offline) { $buildArguments += '--offline' }
            & '.\gradlew.bat' @buildArguments
            if ($LASTEXITCODE -ne 0) { throw "Build falhou: $($target.Module)" }
        } finally { Pop-Location }
        $builtJar = Join-Path $projectRoot "$($target.Module)/build/libs/$($target.Jar)"
        Copy-Item -LiteralPath $builtJar -Destination (Join-Path $artifactDirectory $target.Jar) -Force
        if ($Install) {
            $modsDirectory = Join-Path $env:USERPROFILE "curseforge/minecraft/Instances/$($target.Instance)/mods"
            if (-not (Test-Path -LiteralPath $modsDirectory)) { throw "Instance not found: $modsDirectory" }
            Get-ChildItem -File -LiteralPath $modsDirectory -Filter 'justgashloot-*.jar' | Where-Object { $_.Name -ne $target.Jar } | ForEach-Object {
                $disabledPath = $_.FullName + '.disabled'
                if (Test-Path -LiteralPath $disabledPath) { $disabledPath += '.' + [Guid]::NewGuid().ToString('N') }
                Move-Item -LiteralPath $_.FullName -Destination $disabledPath
            }
            Copy-Item -LiteralPath $builtJar -Destination (Join-Path $modsDirectory $target.Jar) -Force
        }
    }
    $hashLines = @(Get-ChildItem -File -LiteralPath $artifactDirectory -Filter '*.jar' | Get-FileHash -Algorithm SHA256 |
        ForEach-Object { "$($_.Hash.ToLowerInvariant())  $([System.IO.Path]::GetFileName($_.Path))" })
    [System.IO.File]::WriteAllLines((Join-Path $artifactDirectory 'SHA256SUMS.txt'), $hashLines, [System.Text.UTF8Encoding]::new($false))
} finally { $env:JAVA_HOME = $originalJavaHome }
