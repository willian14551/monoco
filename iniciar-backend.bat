@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0backend\iniciar-docker.ps1"
set "resultado=%errorlevel%"
pause
exit /b %resultado%
