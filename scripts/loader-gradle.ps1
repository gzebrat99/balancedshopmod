param([Parameter(Position=0,Mandatory=$true)][ValidateSet('fabric','forge','neoforge')][string]$Loader,[Parameter(Position=1,ValueFromRemainingArguments=$true)][string[]]$Tasks)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME=Join-Path $taskRoot '.tools/jdk-21'
$env:GRADLE_USER_HOME=Join-Path $taskRoot '.gradle-home'
Set-Location $taskRoot
if($Loader -eq 'forge'){& (Join-Path $taskRoot 'ports/forge/gradlew.bat') -p (Join-Path $taskRoot 'ports/forge') @Tasks}
elseif($Loader -eq 'neoforge'){& (Join-Path $taskRoot 'gradlew.bat') -p (Join-Path $taskRoot 'ports/neoforge') @Tasks}
else{& (Join-Path $taskRoot 'gradlew.bat') @Tasks}
exit $LASTEXITCODE
