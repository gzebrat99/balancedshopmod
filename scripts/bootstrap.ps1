$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$toolsRoot = Join-Path $taskRoot '.tools'
New-Item -ItemType Directory -Force $toolsRoot | Out-Null
New-Item -ItemType Directory -Force (Join-Path $taskRoot 'gradle/wrapper') | Out-Null
Invoke-WebRequest 'https://raw.githubusercontent.com/gradle/gradle/v9.2.1/gradlew' -OutFile (Join-Path $taskRoot 'gradlew') -UseBasicParsing
Invoke-WebRequest 'https://raw.githubusercontent.com/gradle/gradle/v9.2.1/gradlew.bat' -OutFile (Join-Path $taskRoot 'gradlew.bat') -UseBasicParsing
Invoke-WebRequest 'https://raw.githubusercontent.com/gradle/gradle/v9.2.1/gradle/wrapper/gradle-wrapper.jar' -OutFile (Join-Path $taskRoot 'gradle/wrapper/gradle-wrapper.jar') -UseBasicParsing
$jdkArchive = Join-Path $toolsRoot 'jdk21.zip'
if (-not (Test-Path (Join-Path $toolsRoot 'jdk-21'))) {
    $jdkReleases = Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
    $jdkPackage = $jdkReleases[0].binary.package
    Invoke-WebRequest $jdkPackage.link -OutFile $jdkArchive -UseBasicParsing
    if ((Get-FileHash $jdkArchive -Algorithm SHA256).Hash.ToLower() -ne $jdkPackage.checksum) { throw 'JDK checksum mismatch' }
    Expand-Archive -LiteralPath $jdkArchive -DestinationPath (Join-Path $toolsRoot 'jdk-extract') -Force
    $jdkFolder = Get-ChildItem (Join-Path $toolsRoot 'jdk-extract') -Directory | Select-Object -First 1
    Move-Item -LiteralPath $jdkFolder.FullName -Destination (Join-Path $toolsRoot 'jdk-21')
}
Write-Output 'Gradle wrapper and checksum-verified Java 21 are ready.'
