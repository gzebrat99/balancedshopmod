param([string[]]$Names)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskRoot=Split-Path $PSScriptRoot -Parent
$sourceJars=Get-ChildItem (Join-Path $taskRoot '.gradle/loom-cache/minecraftMaven/net/minecraft') -Recurse -Filter '*sources.jar'
foreach($sourceJar in $sourceJars) {
    $sourceZip=[IO.Compression.ZipFile]::OpenRead('\\?\'+$sourceJar.FullName)
    foreach($name in $Names) {
        $entry=$sourceZip.GetEntry($name)
        if($null -ne $entry) {
            $reader=[IO.StreamReader]::new($entry.Open())
            Write-Output $reader.ReadToEnd()
            $reader.Dispose()
        }
    }
    $sourceZip.Dispose()
}
