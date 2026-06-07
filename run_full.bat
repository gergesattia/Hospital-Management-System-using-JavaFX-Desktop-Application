@echo off
REM ------------------------------------------------------------
REM run_full.bat - Starts the complete ERP_Project stack
REM ------------------------------------------------------------

REM ----- 1. Clean up lingering processes -----
echo Stopping any lingering Java processes...
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM node.exe >nul 2>&1

REM ----- 2. Set Maven and Java in PATH -----
set "MAVEN_HOME=%~dp0apache-maven-3.9.6"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%;%MAVEN_HOME%\bin"

REM ----- 3. Launch n8n -----
if exist "%~dp0start_n8n.bat" (
    echo Starting n8n...
    start "n8n" cmd /c "%~dp0start_n8n.bat"
) else (
    echo start_n8n.bat not found - skipping n8n startup.
)

REM ----- 4. Launch AI Bridge -----
if exist "%~dp0start_ai.bat" (
    echo Starting AI Bridge...
    start "AI Bridge" cmd /c "%~dp0start_ai.bat"
) else (
    echo start_ai.bat not found - skipping AI Bridge startup.
)

REM ----- 5. Give AI Bridge time to initialize -----
echo Waiting 10 seconds for AI Bridge to start...
ping 127.0.0.1 -n 11 >nul

REM ----- 6. Launch the JavaFX UI -----
echo Launching JavaFX application...
call "%MAVEN_HOME%\bin\mvn.cmd" -f "%~dp0pom.xml" javafx:run

echo All services have been started.
pause
