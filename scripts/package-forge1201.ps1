$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$version='2.5.0'
$name="forge-balanced-shop-1.20.1-$version.jar"
$projectRoot=Join-Path $taskRoot 'versions/forge-legacy'
$jar=Join-Path $projectRoot "build/1.20.1/libs/$name"
$release=Join-Path $taskRoot "dist/$version"
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Entry($zip,[string]$name){
    $entry=$zip.GetEntry($name);if(!$entry){throw "Missing entry $name"}
    $reader=[IO.StreamReader]::new($entry.Open());try{$reader.ReadToEnd()}finally{$reader.Dispose()}
}
$zip=[IO.Compression.ZipFile]::OpenRead($jar)
try {
    $metadata=Read-Entry $zip 'META-INF/mods.toml'
    foreach($value in @('Forge Balanced Shop',$version,'[1.20.1]','[47.3.10,)')){if(!$metadata.Contains($value)){throw "Wrong metadata: $value"}}
    foreach($entry in $zip.Entries){
        if($entry.FullName.StartsWith('dev/eymen/balancedshop/verification/') -or $entry.FullName.StartsWith('assets/create/') -or $entry.FullName -eq 'fabric.mod.json'){throw "Fixture/wrong loader leaked: $($entry.FullName)"}
        if($entry.FullName -like '*Uncrafting*.class' -or $entry.FullName -eq 'data/balancedshop/recipes/uncrafting_table.json'){throw 'Uncraft implementation leaked'}
        if($entry.FullName -match '(CreateTrader|TraderPackets|CreateVillagerMixin).*\.class$'){throw 'Removed Create interface leaked'}
        if($entry.FullName -match '/Vein(MiningService|Packets).*\.class$'){throw 'Removed mining implementation leaked'}
        if($entry.FullName.EndsWith('.class')){
            $stream=$entry.Open();$memory=[IO.MemoryStream]::new()
            try{$stream.CopyTo($memory);$bytes=$memory.ToArray()}finally{$stream.Dispose();$memory.Dispose()}
            if(([int]$bytes[6]*256+[int]$bytes[7]) -ne 61){throw "Java 17 target mismatch: $($entry.FullName)"}
            $constants=[Text.Encoding]::ASCII.GetString($bytes)
            if($constants.Contains('net/fabricmc/')){throw "Fabric dependency leaked: $($entry.FullName)"}
            if($entry.FullName -eq 'dev/eymen/balancedshop/data/DynamicMarket.class' -and !$constants.Contains('m_')){throw 'Missing production SRG mappings'}
        }
    }
    foreach($path in @('keybind/MarketKeybind','menu/ShopMenu','screen/MarketScreen','screen/GuideScreen','localization/TurkishText','network/MarketNetwork','data/EconomyData','data/DynamicMarket','command/EconomyCommands','placement/AreaPlacement','placement/PlacementClient','placement/PlacementScreen','screen/SpawnerStorageScreen','spawner/SpawnerLoot','spawner/SpawnerStorage','mixin/SpawnerStorageMixin','mixin/SpawnerRemovalMixin')){
        if(!$zip.GetEntry("dev/eymen/balancedshop/$path.class")){throw "Missing module $path"}
    }
    $refmap=Read-Entry $zip 'balancedshop.refmap.json'
    if(!$refmap.Contains('m_6765_')){throw 'Invalid mixin remapping'}
    foreach($method in @('m_155761_','m_183515_','m_142466_','m_6810_')){if(!$refmap.Contains($method)){throw "Missing spawner mixin remapping: $method"}}
    if((Read-Entry $zip 'balancedshop.mixins.json').Contains('CreateVillagerMixin')){throw 'Removed villager mixin is still active'}
    $turkish=Read-Entry $zip 'assets/balancedshop/lang/search_tr_tr.json' | ConvertFrom-Json
    if($turkish.'item.minecraft.diamond' -ne 'Elmas'){throw 'Missing Turkish vanilla search index'}
    foreach($language in @('tr_tr','en_us')){
        $ui=Read-Entry $zip "assets/balancedshop/lang/$language.json" | ConvertFrom-Json
        if($ui.'shop.balancedshop.buy' -ne ('Sat'+[char]0x131+'n al')){throw "Wrong Turkish UI: $language"}
    }
}finally{$zip.Dispose()}
New-Item -ItemType Directory -Force (Join-Path $release '1.20.1'),(Join-Path $release 'verification') | Out-Null
foreach($phase in @('server','client')){
    $folder=Join-Path $projectRoot "run/1.20.1/$phase/verification"
    $failure=if($phase -eq 'server'){'FAILED.txt'}else{'CLIENT-FAILED.txt'}
    if(Test-Path -LiteralPath (Join-Path $folder $failure)){throw "$phase verification failed"}
    $report=Get-Content -LiteralPath (Join-Path $folder "$phase.txt") -Raw
    if($report -notmatch '^PASS (\d+) assertions'){throw "Missing $phase verification"}
    $minimum=if($phase -eq 'server'){596}else{103}
    if([int]$Matches[1] -lt $minimum){throw "Incomplete $phase verification"}
    Copy-Item -LiteralPath (Join-Path $folder "$phase.txt") -Destination (Join-Path $release "verification/forge-1.20.1-$phase.txt")
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'run/1.20.1/server/verification/market.txt') -Destination (Join-Path $release 'verification/market.txt')
Copy-Item -LiteralPath $jar -Destination (Join-Path $release "1.20.1/$name")
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/forge1201/README.md') -Destination (Join-Path $release 'KURULUM-VE-MIMARI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/forge1201/TEST-RESULTS.md') -Destination (Join-Path $release 'TEST-SONUCLARI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/forge1201/REHBER-TURKCE.md') -Destination (Join-Path $release 'REHBER-TURKCE.md')
Copy-Item -LiteralPath (Join-Path $projectRoot 'run/1.20.1/client/screenshots/market-dynamic-trade.png') -Destination (Join-Path $release 'market-preview.png')
Copy-Item -LiteralPath (Join-Path $projectRoot 'run/1.20.1/client/screenshots/market-rpg-large.png') -Destination (Join-Path $release 'market-rpg-large.png')
Copy-Item -LiteralPath (Join-Path $projectRoot 'run/1.20.1/client/screenshots/area-placement.png') -Destination (Join-Path $release 'area-placement.png')
Copy-Item -LiteralPath (Join-Path $projectRoot 'run/1.20.1/client/screenshots/area-preview.png') -Destination (Join-Path $release 'area-preview.png')
foreach($screenshot in @('market-turkish.png','guide-turkish.png','spawner-storage.png')){Copy-Item -LiteralPath (Join-Path $projectRoot "run/1.20.1/client/screenshots/$screenshot") -Destination (Join-Path $release $screenshot)}
$hash=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $release 'SHA256SUMS.txt'),"$hash  1.20.1/$name`n")
$destination=Join-Path $taskRoot "dist/Balanced-Shop-$version-Forge-1.20.1.zip"
Compress-Archive -Path (Join-Path $release '*') -DestinationPath $destination -Force
Write-Output "PACKAGED $destination"
Write-Output "SHA256 $hash"
