@echo off
setlocal

if "%~1"=="" (
    echo Usage: run-instance.bat INSTANCE_ID SERVER_PORT [REDIS_URL] [LEADER_LOCK_KEY]
    exit /b 1
)

set "INSTANCE_ID=%~1"
set "SERVER_PORT=%~2"
set "REDIS_URL=%~3"
set "LEADER_LOCK_KEY=%~4"

if "%SERVER_PORT%"=="" set "SERVER_PORT=8080"
if "%REDIS_URL%"=="" set "REDIS_URL=redis://localhost:6379"
if "%LEADER_LOCK_KEY%"=="" set "LEADER_LOCK_KEY=app:leader-lock"

cd /d "%~dp0..\.."
echo Starting %INSTANCE_ID% on port %SERVER_PORT% using %REDIS_URL%
call gradlew.bat :leader-lock-demo:bootRun
exit /b %ERRORLEVEL%
