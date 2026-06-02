$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "D:\Program Files\Java\jdk-25"
$env:GRADLE_USER_HOME = "D:\gradle-home"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat build --offline --console=plain
