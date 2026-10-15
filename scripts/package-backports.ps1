$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$release='2.0.0'
$matrix=Get-Content -LiteralPath (Join-Path $taskRoot 'versions/matrix.json') -Raw | ConvertFrom-Json
$distRoot=Join-Path $taskRoot "dist/$release"
New-Item -ItemType Directory -Force $distRoot | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$hashes=[Collections.Generic.List[string]]::new()
$jarPaths=[Collections.Generic.List[string]]::new()
function Read-Entry($archive,[string]$name) {
    $entry=$archive.GetEntry($name)
    if(!$entry){throw "Missing archive entry: $name"}
    $reader=[IO.StreamReader]::new($entry.Open(),[Text.Encoding]::UTF8)
    try {$reader.ReadToEnd()} finally {$reader.Dispose()}
}
foreach($mc in @('1.20.1','1.21.1')) {
    foreach($loader in @('fabric','forge','neoforge')) {
        $target=$matrix | Where-Object minecraft -eq $mc
        $versionProperty=$loader+'ModVersion'
        $modVersion=if($target.$versionProperty){[string]$target.$versionProperty}else{'1.7.0'}
        $project=if($mc -eq '1.20.1' -and $loader -ne 'fabric'){"$loader-legacy"}else{$loader}
        $projectRoot=Join-Path $taskRoot "versions/$project"
        $name="$loader-balanced-shop-$mc-$modVersion.jar"
        $source=Join-Path $projectRoot "build/$mc/libs/$name"
        if(!(Test-Path -LiteralPath $source -PathType Leaf)){throw "Missing build: $source"}
        $archive=[IO.Compression.ZipFile]::OpenRead($source)
        try {
            $metadataName=if($loader -eq 'fabric'){'fabric.mod.json'}elseif($loader -eq 'neoforge' -and $mc -eq '1.21.1'){'META-INF/neoforge.mods.toml'}else{'META-INF/mods.toml'}
            $metadata=Read-Entry $archive $metadataName
            $label=switch($loader){fabric{'Fabric Balanced Shop'} forge{'Forge Balanced Shop'} neoforge{'NeoForge Balanced Shop'}}
            if(!$metadata.Contains($label) -or !$metadata.Contains($modVersion) -or !$metadata.Contains($mc)){throw "Wrong metadata: $name"}
            if($loader -eq 'neoforge' -and $mc -eq '1.21.1' -and !$metadata.Contains('versionRange="['+$target.neoforge+',)"')){throw "NeoForge dependency range mismatch: $name"}
            if($loader -eq 'forge' -and $mc -eq '1.20.1' -and !$metadata.Contains('versionRange="['+$target.forge+',)"')){throw "Forge dependency range mismatch: $name"}
            if($archive.Entries.Count -lt 100){throw "Incomplete JAR: $name"}
            foreach($entry in $archive.Entries) {
                if($entry.FullName.StartsWith('dev/eymen/balancedshop/verification/') -or $entry.FullName.StartsWith('assets/create/')){throw "Test fixture leaked: $name $($entry.FullName)"}
                if($loader -ne 'fabric' -and $entry.FullName -eq 'fabric.mod.json'){throw "Fabric metadata in native JAR: $name"}
                if($entry.FullName.EndsWith('.class')) {
                    $stream=$entry.Open();$memory=[IO.MemoryStream]::new()
                    try {$stream.CopyTo($memory);$bytes=$memory.ToArray()}finally{$stream.Dispose();$memory.Dispose()}
                    $major=([int]$bytes[6])*256+[int]$bytes[7]
                    $expected=if($mc -eq '1.20.1'){61}else{65}
                    if($major -gt $expected){throw "Incompatible Java class: $name $($entry.FullName) version=$major"}
                    if($entry.FullName -eq 'dev/eymen/balancedshop/TradeEngine.class' -and $major -ne $expected){throw "Incorrect Java target: $name"}
                    $constants=[Text.Encoding]::ASCII.GetString($bytes)
                    if($loader -ne 'fabric' -and $constants.Contains('net/fabricmc/')){throw "Fabric dependency in native JAR: $name $($entry.FullName)"}
                    if($mc -eq '1.20.1' -and $loader -ne 'fabric' -and $entry.FullName -eq 'dev/eymen/balancedshop/TradeEngine.class' -and !$constants.Contains('m_')){throw "Production SRG remapping missing: $name"}
                }
            }
            $null=Read-Entry $archive 'assets/balancedshop/models/item/copper_coin.json'
            $recipeFolder=if($mc -eq '1.20.1'){'recipes'}else{'recipe'}
            if($archive.GetEntry("data/balancedshop/$recipeFolder/uncrafting_table.json")){throw "Reverse crafting recipe leaked: $name"}
            if(@($archive.Entries | Where-Object FullName -Like '*Uncrafting*.class').Count -gt 0){throw "Reverse crafting implementation leaked: $name"}
            $null=Read-Entry $archive 'assets/balancedshop/models/item/mob_spawner.json'
            $null=Read-Entry $archive 'dev/eymen/balancedshop/VeinMiningService.class'
            $null=Read-Entry $archive 'dev/eymen/balancedshop/SpawnerService.class' 
            $lootFolder=if($mc -eq '1.20.1'){'loot_tables'}else{'loot_table'}
            $tagFolder=if($mc -eq '1.20.1'){'blocks'}else{'block'}
            $null=Read-Entry $archive "data/balancedshop/$lootFolder/blocks/uncrafting_table.json"
            $null=Read-Entry $archive "data/minecraft/tags/$tagFolder/mineable/axe.json"
            if(!$archive.GetEntry('assets/balancedshop/textures/item/coin.png')){throw "Coin texture missing: $name"}
            if($mc -eq '1.20.1' -and $loader -ne 'fabric') {
                $refmap=Read-Entry $archive 'balancedshop.refmap.json'
                if(!$refmap.Contains('m_6765_') -or !$refmap.Contains('m_6616_')){throw "Invalid SRG Mixin mappings: $name"}
                foreach($mixin in @('balancedshop.mixins.json','balancedshop.ports.mixins.json')) {
                    if(!(Read-Entry $archive $mixin).Contains('balancedshop.refmap.json')){throw "Missing Mixin refmap declaration: $name"}
                }
            }
        }finally{$archive.Dispose()}
        $versionDir=Join-Path $distRoot $mc
        $reportsDir=Join-Path $distRoot 'verification'
        New-Item -ItemType Directory -Force $versionDir,$reportsDir | Out-Null
        foreach($phase in @('server','client')) {
            $reportRoot=Join-Path $projectRoot "run/$mc/$phase/verification"
            $failure=if($phase -eq 'server'){'FAILED.txt'}else{'CLIENT-FAILED.txt'}
            $report=Join-Path $reportRoot "$phase.txt"
            if((Test-Path -LiteralPath (Join-Path $reportRoot $failure)) -or !(Test-Path -LiteralPath $report)){throw "Verification missing/failed: $name $phase"}
            $expected=if($phase -eq 'server'){if($mc -eq '1.20.1'){189}else{192}}else{35}
            if(!([IO.File]::ReadAllText($report).StartsWith("PASS $expected assertions"))){throw "Incomplete verification: $name $phase"}
            if($loader -eq 'neoforge' -and $mc -eq '1.21.1') {
                $gameLog=Join-Path $projectRoot "run/$mc/$phase/logs/latest.log"
                if(!([IO.File]::ReadAllText($gameLog).Contains("NeoForge mod loading, version $($target.neoforge),"))){throw "Wrong NeoForge runtime in verification: $name $phase"}
            }
            if($loader -eq 'forge' -and $mc -eq '1.20.1') {
                $gameLog=Join-Path $projectRoot "run/$mc/$phase/logs/latest.log"
                if(!([IO.File]::ReadAllText($gameLog).Contains("Forge mod loading, version $($target.forge),"))){throw "Wrong Forge runtime in verification: $name $phase"}
            }
            Copy-Item -LiteralPath $report -Destination (Join-Path $reportsDir "$loader-$mc-$phase.txt")
        }
        if($loader -eq 'fabric') {
            $junit=Join-Path $projectRoot "build/$mc/test-results/test/TEST-dev.eymen.balancedshop.EconomyTest.xml"
            [xml]$result=[IO.File]::ReadAllText($junit)
            if([int]$result.testsuite.tests -ne 14 -or [int]$result.testsuite.failures -ne 0 -or [int]$result.testsuite.errors -ne 0 -or [int]$result.testsuite.skipped -ne 0){throw "Economy tests failed/incomplete: $mc"}
            Copy-Item -LiteralPath $junit -Destination (Join-Path $reportsDir "fabric-$mc-economy.xml")
        }
        $destination=Join-Path $versionDir $name
        Copy-Item -LiteralPath $source -Destination $destination
        $sourceHash=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
        if((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sourceHash){throw "Copy hash mismatch: $name"}
        $relative="$mc/$name"
        $hashes.Add("$sourceHash  $relative")
        $jarPaths.Add($relative)
    }
}
foreach($mc in @('1.20.1','1.21.1')) {
    $csv=Join-Path $taskRoot "versions/fabric/run/$mc/server/world/balancedshop/prices.csv"
    $prices=Get-Content -LiteralPath $csv | Where-Object { $_.StartsWith('item_id,') -or $_.StartsWith('minecraft:') }
    [IO.File]::WriteAllLines((Join-Path $distRoot "$mc/vanilla-prices.csv"),$prices,[Text.UTF8Encoding]::new($false))
    Copy-Item -LiteralPath (Join-Path $taskRoot "versions/fabric/run/$mc/server/world/balancedshop/spawners.csv") -Destination (Join-Path $distRoot "$mc/spawner-prices.csv")
}
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/README.md') -Destination (Join-Path $distRoot 'KURULUM.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'versions/TEST-RESULTS.md') -Destination (Join-Path $distRoot 'TEST-SONUCLARI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'LICENSE') -Destination (Join-Path $distRoot 'LICENSE')
Copy-Item -LiteralPath (Join-Path $taskRoot 'ASSET-NOTES.md') -Destination (Join-Path $distRoot 'COIN-GORSELI.md')
[IO.File]::WriteAllLines((Join-Path $distRoot 'SHA256.txt'),$hashes,[Text.UTF8Encoding]::new($false))
$zipPath=Join-Path $taskRoot "dist/Balanced-Shop-$release-1.20.1-1.21.1-All-Loaders.zip"
if(Test-Path -LiteralPath $zipPath){Remove-Item -LiteralPath $zipPath}
[IO.Compression.ZipFile]::CreateFromDirectory($distRoot,$zipPath)
$zip=[IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    if(@($zip.Entries | Where-Object FullName -Like '*.jar').Count -ne 6){throw 'Release must contain exactly six JARs'}
    foreach($path in $jarPaths) {
        $entry=$zip.GetEntry($path)
        if(!$entry){$entry=$zip.GetEntry($path.Replace('/','\'))}
        if(!$entry){throw "JAR missing from ZIP: $path"}
        $stream=$entry.Open();$sha=[Security.Cryptography.SHA256]::Create()
        try {$hash=([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','').ToLowerInvariant()}finally{$stream.Dispose();$sha.Dispose()}
        if($hash -ne (Get-FileHash -LiteralPath (Join-Path $distRoot $path) -Algorithm SHA256).Hash.ToLowerInvariant()){throw "ZIP hash mismatch: $path"}
    }
}finally{$zip.Dispose()}
Get-ChildItem -LiteralPath $distRoot -Recurse -Filter '*.jar' | Select-Object Name,Length
Write-Output "Verified release: $zipPath"
