param([ValidateSet('fabric','forge','neoforge')][string]$Loader='forge',[string[]]$Minecraft=@('1.20.1'))
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $taskRoot
$matrix=Get-Content -LiteralPath 'versions/matrix.json' -Raw | ConvertFrom-Json
if(!$Minecraft){$Minecraft=@('1.20.1')}
foreach($mc in $Minecraft){
    $row=$matrix | Where-Object minecraft -eq $mc
    if(!$row){throw "Unknown version: $mc"}
    if($Loader -eq 'forge' -and !$row.forge){Write-Output "UNAVAILABLE $Loader $mc";continue}
    $log=Join-Path $taskRoot ".tools/multiversion/$Loader-$mc.log"
    Write-Output "BUILDING $Loader $mc"
    $ErrorActionPreference='Continue'
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'multiversion-gradle.ps1') -Loader $Loader -Minecraft $mc build --console=plain *> $log
    $buildExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($buildExit -ne 0){Get-Content -LiteralPath $log -Tail 65;exit $buildExit}
    Write-Output "COMPILED $Loader $mc"
}
