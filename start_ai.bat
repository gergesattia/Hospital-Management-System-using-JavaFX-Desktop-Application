@echo off
title MediCore AI Bridge
echo ==========================================
echo       MediCore AI Bridge Launcher
echo ==========================================
echo.

cd /d "%~dp0src\main\resources\ai"

:: Suppress TensorFlow noise and force CPU mode to prevent GPU hang
set TF_CPP_MIN_LOG_LEVEL=3
set TF_ENABLE_ONEDNN_OPTS=0
set CUDA_VISIBLE_DEVICES=-1

echo.
echo [INFO] Starting AI Bridge (CPU mode)...
echo ==========================================
venv\Scripts\python.exe ai_bridge.py

pause
