$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$releaseVersion = '1.6.0'
$distRoot = Join-Path $taskRoot ('dist/' + $releaseVersion)
New-Item -ItemType Directory -Force -Path $distRoot | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$hashLines = [Collections.Generic.List[string]]::new()
foreach ($loader in @('fabric','forge','neoforge')) {
    $jarName = $loader + '-balanced-shop-1.21.11-' + $releaseVersion + '.jar'
    $buildFolder = if ($loader -eq 'fabric') { 'build/libs' } else { 'ports/' + $loader + '/build/libs' }
    $sourceJar = Join-Path $taskRoot ($buildFolder + '/' + $jarName)
    if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) { throw ('Missing build: ' + $sourceJar) }
    $archive = [IO.Compression.ZipFile]::OpenRead($sourceJar)
    try {
        $metadataName = switch ($loader) { 'fabric' { 'fabric.mod.json' } 'forge' { 'META-INF/mods.toml' } 'neoforge' { 'META-INF/neoforge.mods.toml' } }
        $entry = $archive.GetEntry($metadataName)
        if ($null -eq $entry) { throw ($loader + ' metadata missing') }
        $reader = [IO.StreamReader]::new($entry.Open(), [Text.Encoding]::UTF8)
        try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if (-not $metadata.Contains($releaseVersion)) { throw ($loader + ' version mismatch') }
        $label = switch ($loader) { 'fabric' { 'Fabric Balanced Shop' } 'forge' { 'Forge Balanced Shop' } 'neoforge' { 'NeoForge Balanced Shop' } }
        if (-not $metadata.Contains($label)) { throw ($loader + ' display name mismatch') }
        if ($archive.Entries.Count -lt 100) { throw ($loader + ' incomplete archive') }
        foreach ($item in $archive.Entries) {
            if ($item.FullName.StartsWith('dev/eymen/balancedshop/verification/') -or $item.FullName.StartsWith('assets/create/')) { throw ('Development fixture leaked: ' + $item.FullName) }
            if ($loader -ne 'fabric' -and $item.FullName -eq 'fabric.mod.json') { throw ('Wrong loader metadata: ' + $loader) }
            if ($loader -ne 'fabric' -and $item.FullName.EndsWith('.class')) {
                $stream = $item.Open(); $bytes = [IO.MemoryStream]::new()
                try { $stream.CopyTo($bytes); $constants = [Text.Encoding]::ASCII.GetString($bytes.ToArray()) } finally { $stream.Dispose(); $bytes.Dispose() }
                if ($constants.Contains('net/fabricmc/')) { throw ('Fabric API dependency in native port: ' + $item.FullName) }
            }
        }
    } finally { $archive.Dispose() }
    if ($loader -ne 'fabric') {
        $reportRoot = Join-Path $taskRoot ('ports/' + $loader + '/run/client/verification')
        if (Test-Path -LiteralPath (Join-Path $reportRoot 'FAILED.txt')) { throw ($loader + ' native verification failed') }
        foreach ($report in @('server.txt','client.txt')) {
            $reportPath = Join-Path $reportRoot $report
            if (-not (Test-Path -LiteralPath $reportPath)) { throw ($loader + ' verification report missing') }
            if (-not ([IO.File]::ReadAllText($reportPath).StartsWith('PASS:'))) { throw ($loader + ' verification incomplete') }
            Copy-Item -LiteralPath $reportPath -Destination (Join-Path $distRoot ($loader + '-' + $report))
        }
    }
    $target = Join-Path $distRoot $jarName
    Copy-Item -LiteralPath $sourceJar -Destination $target
    $sourceHash = (Get-FileHash -LiteralPath $sourceJar -Algorithm SHA256).Hash.ToLowerInvariant()
    $targetHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($sourceHash -ne $targetHash) { throw ($loader + ' copy hash mismatch') }
    $hashLines.Add($targetHash + '  ' + $jarName)
}
Copy-Item -LiteralPath (Join-Path $taskRoot 'README.md') -Destination (Join-Path $distRoot 'KURULUM.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'TEST-RESULTS.md') -Destination (Join-Path $distRoot 'TEST-SONUCLARI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'ASSET-NOTES.md') -Destination (Join-Path $distRoot 'COIN-GORSELI.md')
Copy-Item -LiteralPath (Join-Path $taskRoot 'LICENSE') -Destination (Join-Path $distRoot 'LICENSE')
foreach ($name in @('balancedshop.example.json','buyu-kitabi-fiyatlari.csv','vanilla-fiyatlar.csv','vanilla-ve-create-fiyatlar.csv')) {
    Copy-Item -LiteralPath (Join-Path $taskRoot ('dist/1.5.0/' + $name)) -Destination (Join-Path $distRoot $name)
}
[IO.File]::WriteAllLines((Join-Path $distRoot 'SHA256.txt'), $hashLines, [Text.UTF8Encoding]::new($false))
$zipPath = Join-Path $taskRoot ('dist/Balanced-Shop-' + $releaseVersion + '-All-Loaders.zip')
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath }
[IO.Compression.ZipFile]::CreateFromDirectory($distRoot, $zipPath)
Get-ChildItem -LiteralPath $distRoot | Select-Object Name,Length
Write-Output ('Release ZIP: ' + $zipPath)
