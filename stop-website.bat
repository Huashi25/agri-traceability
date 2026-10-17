@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo     Stopping Website...
echo ========================================
echo.
"C:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.exe" -ExecutionPolicy Bypass -File "%~dp0stop-website.ps1"
echo.
echo Press any key to close...
pause >nul