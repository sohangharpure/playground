@echo off

echo Stopping Redis leader-lock instance windows...
taskkill /FI "WINDOWTITLE eq redis-leader-lock-*" /T /F >nul 2>&1
if errorlevel 1 echo No matching instance windows were found.
