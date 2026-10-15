param(
    [Parameter(Mandatory=$true)][ValidateSet('fabric','forge','neoforge')][string]$Loader,
    [Parameter(Mandatory=$true)][string]$Minecraft,
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$Tasks
)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME=Join-Path $taskRoot '.tools/jdk-21'
$env:GRADLE_USER_HOME=Join-Path $taskRoot '.gradle-home'
Set-Location -LiteralPath $taskRoot
$legacy=($Minecraft -eq '1.20.1' -and $Loader -ne 'fabric')
if($Loader -eq 'forge' -and !$legacy){$wrapper=Join-Path $taskRoot 'ports/forge/gradlew.bat'}else{$wrapper=Join-Path $taskRoot 'gradlew.bat'}
$projectName=if($legacy){"$Loader-legacy"}else{$Loader}
& $wrapper -p (Join-Path $taskRoot "versions/$projectName") "-Pmc=$Minecraft" @Tasks
exit $LASTEXITCODE
