param([ValidateSet('fabric','forge','neoforge')][string]$Loader,[ValidateSet('1.20.1','1.21.1')][string]$Minecraft,[ValidateSet('server','client','both')][string]$Phase='both')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $taskRoot
$projectName=if($Minecraft -eq '1.20.1' -and $Loader -ne 'fabric'){"$Loader-legacy"}else{$Loader}
$projectRoot=Join-Path $taskRoot "versions/$projectName"
$serverRoot=Join-Path $projectRoot "run/$Minecraft/server"
$clientRoot=Join-Path $projectRoot "run/$Minecraft/client"
if($Phase -in @('server','both')){
    foreach($marker in @('server.txt','FAILED.txt')) { Remove-Item -LiteralPath (Join-Path $serverRoot "verification/$marker") -ErrorAction SilentlyContinue }
    New-Item -ItemType Directory -Force $serverRoot | Out-Null
    [IO.File]::WriteAllText((Join-Path $serverRoot 'eula.txt'),'eula=true')
    $port=25580+(@('fabric','forge','neoforge').IndexOf($Loader)*2)+[int]($Minecraft -eq '1.21.1')
    $properties="server-ip=127.0.0.1`nserver-port=$port`nonline-mode=false`nlevel-name=world`nlevel-type=minecraft:flat`ngenerator-settings={`"layers`": [{`"block`":`"minecraft:bedrock`",`"height`":1},{`"block`":`"minecraft:dirt`",`"height`":2},{`"block`":`"minecraft:grass_block`",`"height`":1}],`"biome`":`"minecraft:plains`",`"structure_overrides`": []}`nspawn-protection=0`nview-distance=2`nsimulation-distance=2`n"
    [IO.File]::WriteAllText((Join-Path $serverRoot 'server.properties'),$properties)
    $ErrorActionPreference='Continue'
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'multiversion-gradle.ps1') -Loader $Loader -Minecraft $Minecraft runServer -Pverify --console=plain
    $checkExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($checkExit -ne 0){exit $checkExit}
    if(!(Test-Path (Join-Path $serverRoot 'verification/server.txt')) -or (Test-Path (Join-Path $serverRoot 'verification/FAILED.txt'))){throw 'Server verification did not pass'}
    Get-Content (Join-Path $serverRoot 'verification/server.txt')
}
if($Phase -in @('client','both')){
    foreach($marker in @('client.txt','CLIENT-FAILED.txt')) { Remove-Item -LiteralPath (Join-Path $clientRoot "verification/$marker") -ErrorAction SilentlyContinue }
    $saves=Join-Path $clientRoot 'saves'
    New-Item -ItemType Directory -Force $saves | Out-Null
    $world=Join-Path $saves 'verification'
    if(!(Test-Path $world)){Copy-Item -LiteralPath (Join-Path $serverRoot 'world') -Destination $world -Recurse}
    $ErrorActionPreference='Continue'
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'multiversion-gradle.ps1') -Loader $Loader -Minecraft $Minecraft runClient -Pverify --console=plain
    $checkExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($checkExit -ne 0){exit $checkExit}
    if(!(Test-Path (Join-Path $clientRoot 'verification/client.txt')) -or (Test-Path (Join-Path $clientRoot 'verification/CLIENT-FAILED.txt'))){throw 'Client verification did not pass'}
    Get-Content (Join-Path $clientRoot 'verification/client.txt')
}
