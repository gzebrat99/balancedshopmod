$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$distRoot=Join-Path $taskRoot 'dist/1.5.0'
New-Item -ItemType Directory -Force $distRoot | Out-Null
$jar=Join-Path $taskRoot 'build/libs/balanced-shop-1.21.11-1.5.0.jar'
$target=Join-Path $distRoot 'balanced-shop-1.21.11-1.5.0.jar'
Copy-Item -LiteralPath $jar -Destination $target
Copy-Item -LiteralPath (Join-Path $taskRoot 'README.md') -Destination (Join-Path $distRoot 'KURULUM.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'TEST-RESULTS.md') -Destination (Join-Path $distRoot 'TEST-SONUCLARI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'ASSET-NOTES.md') -Destination (Join-Path $distRoot 'COIN-GORSELI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/config/balancedshop.json') -Destination (Join-Path $distRoot 'balancedshop.example.json')
$bookPrices=Get-Content -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/saves/New World/balancedshop/enchantments.csv') | Where-Object {$_ -notmatch '^shopcompat_test:'}
[IO.File]::WriteAllLines((Join-Path $distRoot 'buyu-kitabi-fiyatlari.csv'),[string[]]$bookPrices,[Text.UTF8Encoding]::new($false))
$vanilla=Get-Content -LiteralPath (Join-Path $taskRoot 'build/verification/vanilla-prices.csv') | Where-Object {$_ -match '^(item_id,|minecraft:)'}
[IO.File]::WriteAllLines((Join-Path $distRoot 'vanilla-fiyatlar.csv'),[string[]]$vanilla,[Text.UTF8Encoding]::new($false))
$withCreate=Get-Content -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/saves/New World/balancedshop/prices.csv') | Where-Object {$_ -notmatch '^shopcompat_test:'}
[IO.File]::WriteAllLines((Join-Path $distRoot 'vanilla-ve-create-fiyatlar.csv'),[string[]]$withCreate,[Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots/0000_balanced-shop-catalogue.png') -Destination (Join-Path $distRoot 'magaza.png')
Copy-Item -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots/0001_balanced-shop-create-trade.png') -Destination (Join-Path $distRoot 'create-alis-satis.png')
Copy-Item -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots/0002_uncrafting-table-preview.png') -Destination (Join-Path $distRoot 'uncrafting-table.png')
Copy-Item -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots/0003_uncrafting-create-preview.png') -Destination (Join-Path $distRoot 'uncrafting-create.png')
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_sell-amount-options.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'satis-secenekleri.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_sell-all-complete.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'toplu-satis.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_spawner-spawn-egg-purchase.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'spawner-yumurta-alis.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_all-enchantment-levels.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'tum-buyuler.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_unbreaking-iii-purchase.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'unbreaking-3.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_lucky-upgrade-preview.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'sansli-yukseltici.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_lucky-enchant-large.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'sansli-buyu.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_inventory-sell-tooltip.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'envanter-satis-degeri.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_bulk-256-crafting-to-diamond.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot '256-crafting-1-elmas.png')}
Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter '*_enchanted-tool-sale-value.png' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot 'buyulu-hasarli-alet-satisi.png')}
Copy-Item -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/logs/latest.log') -Destination (Join-Path $taskRoot 'build/verification/create-game-test.log')
foreach($name in @('coin-wallet-large','coin-wallet-small','create-trader-small','create-trader-large')){
    Get-ChildItem -LiteralPath (Join-Path $taskRoot 'build/run/clientGameTest/screenshots') -Filter ('*_'+$name+'.png') | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $distRoot ($name+'.png'))}
}
$hash=(Get-FileHash $target -Algorithm SHA256).Hash.ToLower()
[IO.File]::WriteAllText((Join-Path $distRoot 'SHA256.txt'),$hash+'  balanced-shop-1.21.11-1.5.0.jar'+[Environment]::NewLine,[Text.UTF8Encoding]::new($false))
Get-ChildItem $distRoot | Select-Object Name,Length
