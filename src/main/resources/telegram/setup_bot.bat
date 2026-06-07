@echo off
cd /d "%~dp0"
echo ==============================================
echo   MediCore Telegram Bot Environment Setup
echo ==============================================
echo.

if not exist venv (
    echo [INFO] Creating Python virtual environment...
    python -m venv venv
    if errorlevel 1 (
        echo [ERROR] Failed to create virtual environment. Is Python installed and in PATH?
        pause
        exit /b 1
    )
) else (
    echo [INFO] Virtual environment already exists.
)

echo.
echo [INFO] Upgrading pip...
venv\Scripts\python.exe -m pip install --upgrade pip

echo.
echo [INFO] Installing required packages...
venv\Scripts\pip.exe install -r requirements.txt
if errorlevel 1 (
    echo [ERROR] Dependency installation failed.
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Environment is fully prepared!
echo ==============================================
exit /b 0
