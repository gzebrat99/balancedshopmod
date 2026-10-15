$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = Join-Path $taskRoot '.tools/jdk-21'
$env:GRADLE_USER_HOME = Join-Path $taskRoot '.gradle-home'
Set-Location $taskRoot
& (Join-Path $taskRoot 'gradlew.bat') @args
exit $LASTEXITCODE
