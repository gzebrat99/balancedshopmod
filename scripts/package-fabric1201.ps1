param()
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$version='2.5.1'
$name="fabric-balanced-shop-1.20.1-$version.jar"
$projectRoot=Join-Path $taskRoot 'versions/fabric'
$jar=Join-Path $projectRoot "build/1.20.1/libs/$name"
$release=Join-Path $taskRoot "dist/fabric-$version"
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Entry($zip,[string]$path){
    $entry=$zip.GetEntry($path);if(!$entry){throw "Missing entry $path"}
    $reader=[IO.StreamReader]::new($entry.Open());try{$reader.ReadToEnd()}finally{$reader.Dispose()}
}
$zip=[IO.Compression.ZipFile]::OpenRead($jar)
try{
    $metadata=Read-Entry $zip 'fabric.mod.json' | ConvertFrom-Json
    if($metadata.depends.fabricloader -ne '>=0.16.9'){throw 'Wrong Fabric Loader compatibility range'}
    if($metadata.version -ne $version -or $metadata.name -ne 'Fabric Balanced Shop' -or $metadata.depends.minecraft -ne '1.20.1' -or $metadata.depends.java -ne '>=17' -or $metadata.depends.'fabric-api' -ne '>=0.92.2'){throw 'Wrong Fabric metadata'}
    if($zip.GetEntry('META-INF/mods.toml') -or $zip.GetEntry('META-INF/neoforge.mods.toml')){throw 'Wrong loader metadata'}
    foreach($entry in $zip.Entries){
        if($entry.FullName.StartsWith('dev/eymen/balancedshop/verification/') -or $entry.FullName.StartsWith('assets/create/')){throw "Verification fixture leaked: $($entry.FullName)"}
        if($entry.FullName -match '(CreateTrader|TraderPackets|CreateVillagerMixin|/Vein(MiningService|Packets)).*\.class$'){throw "Retired module leaked: $($entry.FullName)"}
        if($entry.FullName.EndsWith('.class')){
            $stream=$entry.Open();$memory=[IO.MemoryStream]::new();try{$stream.CopyTo($memory);$bytes=$memory.ToArray()}finally{$stream.Dispose();$memory.Dispose()}
            if(([int]$bytes[6]*256+[int]$bytes[7]) -ne 61){throw "Java 17 target mismatch: $($entry.FullName)"}
            $constants=[Text.Encoding]::ASCII.GetString($bytes)
            if($constants.Contains('net/minecraftforge/') -or $constants.Contains('net/neoforged/')){throw "Other loader dependency leaked: $($entry.FullName)"}
            if($entry.FullName -eq 'dev/eymen/balancedshop/spawner/SpawnerLoot.class' -and !$constants.Contains('net/minecraft/class_')){throw 'Unmapped development JAR'}
        }
    }
    foreach($path in @('spawner/SpawnerLoot','spawner/SpawnerStorage','mixin/SpawnerStorageMixin','mixin/SpawnerRemovalMixin','mixin/SpawnerUseMixin','mixin/client/FabricPlacementMixin','mixin/client/FabricScreenMixin','screen/SpawnerStorageScreen','screen/MarketScreen','screen/GuideScreen','network/FabricTransport','data/EconomyData','placement/AreaPlacement','placement/PlacementClient','localization/TurkishText')){
        if(!$zip.GetEntry("dev/eymen/balancedshop/$path.class")){throw "Missing module $path"}
    }
    $mixins=Read-Entry $zip 'balancedshop.mixins.json' | ConvertFrom-Json
    if($mixins.compatibilityLevel -ne 'JAVA_17' -or $mixins.mixins -notcontains 'SpawnerStorageMixin' -or $mixins.client -notcontains 'client.FabricPlacementMixin'){throw 'Missing Fabric mixins'}
    $turkish=Read-Entry $zip 'assets/balancedshop/lang/search_tr_tr.json' | ConvertFrom-Json
    if($turkish.'item.minecraft.diamond' -ne 'Elmas'){throw 'Missing Turkish item index'}
    foreach($language in @('tr_tr','en_us')){
        $ui=Read-Entry $zip "assets/balancedshop/lang/$language.json" | ConvertFrom-Json
        if($ui.'shop.balancedshop.buy' -ne ('Sat'+[char]0x131+'n al')){throw "Missing Turkish UI $language"}
    }
}finally{$zip.Dispose()}
New-Item -ItemType Directory -Force (Join-Path $release '1.20.1'),(Join-Path $release 'verification') | Out-Null
foreach($phase in @('server','client')){
    $folder=Join-Path $projectRoot "run/1.20.1/$phase/verification"
    $runtimeLog=Get-Content -LiteralPath (Join-Path $projectRoot "run/1.20.1/$phase/logs/latest.log") -Raw
    if($runtimeLog -notmatch 'Loading Minecraft 1\.20\.1 with Fabric Loader 0\.16\.9' -or $runtimeLog -notmatch 'balancedshop 2\.5\.1' -or $runtimeLog -notmatch 'fabric-api 0\.92\.2\+1\.20\.1'){throw "Wrong $phase runtime compatibility test environment"}
    $failure=if($phase -eq 'server'){'FAILED.txt'}else{'CLIENT-FAILED.txt'}
    if(Test-Path -LiteralPath (Join-Path $folder $failure)){throw "$phase verification failed"}
    $report=Get-Content -LiteralPath (Join-Path $folder "$phase.txt") -Raw
    if($report -notmatch '^PASS (\d+) assertions'){throw "Missing $phase verification"}
    $minimum=if($phase -eq 'server'){597}else{103}
    if([int]$Matches[1] -lt $minimum){throw "Incomplete $phase verification"}
    Copy-Item -LiteralPath (Join-Path $folder "$phase.txt") -Destination (Join-Path $release "verification/fabric-1.20.1-$phase.txt")
}
Copy-Item -LiteralPath $jar -Destination (Join-Path $release "1.20.1/$name")
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/fabric1201/README.md') -Destination (Join-Path $release 'KURULUM.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/fabric1201/TEST-RESULTS.md') -Destination (Join-Path $release 'TEST-SONUCLARI.md')
$guide=[IO.File]::ReadAllText((Join-Path $taskRoot 'versions/forge1201/REHBER-TURKCE.md')).Replace('1.20.1 / Forge','1.20.1 / Fabric').Replace('2.5.0',$version)
[IO.File]::WriteAllText((Join-Path $release 'REHBER-TURKCE.md'),$guide,[Text.UTF8Encoding]::new($false))
foreach($screenshot in @('market-turkish.png','guide-turkish.png','spawner-storage.png','area-preview.png')){Copy-Item -LiteralPath (Join-Path $projectRoot "run/1.20.1/client/screenshots/$screenshot") -Destination (Join-Path $release $screenshot)}
$hash=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $release 'SHA256SUMS.txt'),"$hash  1.20.1/$name`n")
$destination=Join-Path $taskRoot "dist/Balanced-Shop-$version-Fabric-1.20.1.zip"
Compress-Archive -Path (Join-Path $release '*') -DestinationPath $destination -Force
Write-Output "PACKAGED $destination"
Write-Output "SHA256 $hash"
