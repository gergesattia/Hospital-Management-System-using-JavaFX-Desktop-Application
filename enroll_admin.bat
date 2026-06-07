@echo off
setlocal

echo ==========================================
echo    MediCore Admin Emergency Enrollment
echo ==========================================

set /p USERNAME="Enter new admin username: "

if "%USERNAME%"=="" (
    echo Username cannot be empty.
    pause
    exit /b
)

echo Starting camera...
set PYTHONPATH=%~dp0src\main\resources\ai
call "%~dp0src\main\resources\ai\venv\Scripts\python.exe" "%~dp0src\main\resources\ai\enroll_standalone.py" %USERNAME%

echo.
pause
