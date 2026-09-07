@echo off
setlocal

set "CONTAINER=%~1"
set "DURATION=%~2"

if "%CONTAINER%"=="" set "CONTAINER=leader-lock-redis"
if "%DURATION%"=="" set "DURATION=15"

for /f "delims=" %%S in ('docker inspect -f "{{.State.Status}}" "%CONTAINER%" 2^>nul') do set "STATUS=%%S"
if not defined STATUS (
    echo Container "%CONTAINER%" was not found.
    echo Usage: simulate-redis-outage.bat [CONTAINER_NAME] [SECONDS]
    exit /b 1
)

if /I not "%STATUS%"=="running" (
    echo Container "%CONTAINER%" is not running. Current state: %STATUS%
    exit /b 1
)

echo Stopping Redis container "%CONTAINER%" for %DURATION% seconds...
docker stop "%CONTAINER%"
if errorlevel 1 exit /b 1

timeout /t %DURATION% /nobreak >nul

echo Starting Redis container "%CONTAINER%"...
docker start "%CONTAINER%"
if errorlevel 1 exit /b 1

echo Redis outage simulation complete.
endlocal
