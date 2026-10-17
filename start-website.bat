@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo     Starting Website...
echo ========================================
echo.
"C:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.exe" -ExecutionPolicy Bypass -File "%~dp0start-website.ps1"
echo.
if %errorlevel% neq 0 (
    echo [ERROR] Startup failed! Error code: %errorlevel%
)
echo.
echo Press any key to close...
pause >nul