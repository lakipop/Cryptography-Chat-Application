@echo off
echo ========================================
echo Starting Chat Server...
echo ========================================
cd /d "%~dp0"
set PATH=C:\Maven\apache-maven-3.9.11\bin;%PATH%
start "Chat Server" cmd /k "mvn javafx:run@run-server"
timeout /t 3 /nobreak >nul
echo.
echo ========================================
echo Starting Chat Client...
echo ========================================
start "Chat Client" cmd /k "mvn javafx:run@run-client"
echo.
echo Both windows should open shortly!
echo Press any key to close this window...
pause >nul
