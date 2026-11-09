@ECHO OFF
SETLOCAL
SET "MAVEN_VERSION=3.9.9"
SET "WRAPPER_DIR=%~dp0.mvn\wrapper"
SET "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
SET "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

IF NOT EXIST "%MAVEN_CMD%" (
    ECHO [Maven Wrapper] Baixando Apache Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference = 'Stop'; " ^
      "$url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; " ^
      "$zip = '%WRAPPER_DIR%\maven.zip'; " ^
      "New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null; " ^
      "Invoke-WebRequest -Uri $url -OutFile $zip; " ^
      "Expand-Archive -Path $zip -DestinationPath '%WRAPPER_DIR%' -Force; " ^
      "Remove-Item $zip -Force"
)

CALL "%MAVEN_CMD%" %*
EXIT /B %ERRORLEVEL%

