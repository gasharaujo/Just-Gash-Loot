$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot 'verification/runtime/core'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$sourceRoot = Join-Path $projectRoot 'common/src/main/java/net/gasharaujo/justgashloot'
& javac --release 17 -encoding UTF-8 -d $outputDirectory (Join-Path $sourceRoot 'BulkInventory.java') (Join-Path $sourceRoot 'LootRules.java') (Join-Path $projectRoot 'verification/CoreChecks.java')
if ($LASTEXITCODE -ne 0) { throw 'Core compilation failed' }
$result = & java -cp $outputDirectory net.gasharaujo.justgashloot.CoreChecks
if ($LASTEXITCODE -ne 0) { throw 'Core checks failed' }
Write-Output $result
if ($result -notmatch 'PASS: (\d+) core checks') { throw 'Core result missing' }
@{ passed = $true; checks = [int]$Matches[1] } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDirectory 'results.json') -Encoding utf8
