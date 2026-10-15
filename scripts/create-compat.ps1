$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$compatRoot=Join-Path $taskRoot '.tools/compat'
New-Item -ItemType Directory -Force $compatRoot | Out-Null
$headers=@{'User-Agent'='BalancedShop-1.21.11/local-compatibility-test'}
$downloaded=@{}
$manifest=[Collections.Generic.List[object]]::new()
function Get-CompatibleVersion([string]$project) {
    $uri='https://api.modrinth.com/v2/project/'+$project+'/version?game_versions=%5B%221.21.11%22%5D&loaders=%5B%22fabric%22%5D'
    $versions=Invoke-RestMethod $uri -Headers $headers
    $version=$versions | Where-Object {$_.version_type -eq 'release'} | Select-Object -First 1
    if($null -eq $version) {$version=$versions | Select-Object -First 1}
    if($null -eq $version) {throw ('No Fabric 1.21.11 version for '+$project)}
    return $version
}
function Save-Version($version) {
    if($downloaded.ContainsKey($version.id)) {return}
    $downloaded[$version.id]=$true
    $file=$version.files | Where-Object {$_.primary} | Select-Object -First 1
    if($null -eq $file) {$file=$version.files[0]}
    if([IO.Path]::GetFileName($file.filename) -ne $file.filename) {throw 'Unsafe download filename'}
    $path=Join-Path $compatRoot $file.filename
    Invoke-WebRequest $file.url -OutFile $path -Headers $headers -UseBasicParsing
    if((Get-FileHash $path -Algorithm SHA512).Hash.ToLower() -ne $file.hashes.sha512) {throw ('Checksum mismatch: '+$file.filename)}
    $manifest.Add(@{project=$version.project_id;version=$version.version_number;file=$file.filename;sha512=$file.hashes.sha512})
    foreach($dependency in $version.dependencies) {
        if($dependency.dependency_type -ne 'required' -or $dependency.project_id -eq 'P7dR8mSH') {continue}
        if($dependency.version_id) {$required=Invoke-RestMethod ('https://api.modrinth.com/v2/version/'+$dependency.version_id) -Headers $headers}
        else {$required=Get-CompatibleVersion $dependency.project_id}
        Save-Version $required
    }
}
Save-Version (Get-CompatibleVersion 'create-fly')
$manifest | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $compatRoot 'manifest.json') -Encoding UTF8
$manifest | Select-Object project,version,file | Format-Table -AutoSize
