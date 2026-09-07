@echo off
setlocal EnableDelayedExpansion

set "COUNT=%~1"
set "BASE_PORT=%~2"
set "REDIS_URL=%~3"
set "LEADER_LOCK_KEY=%~4"

if "%COUNT%"=="" set "COUNT=3"
if "%BASE_PORT%"=="" set "BASE_PORT=8080"
if "%REDIS_URL%"=="" set "REDIS_URL=redis://localhost:6379"
if "%LEADER_LOCK_KEY%"=="" set "LEADER_LOCK_KEY=app:leader-lock"

for /L %%N in (1,1,%COUNT%) do (
    set /A PORT=BASE_PORT+%%N-1
    set "INSTANCE_ID=local-%%N"
    echo Launching !INSTANCE_ID! on port !PORT!
    start "redis-leader-lock-%%N" cmd /k call "%~dp0run-instance.bat" "!INSTANCE_ID!" "!PORT!" "%REDIS_URL%" "%LEADER_LOCK_KEY%"
)

echo.
echo Started %COUNT% instances.
echo Use check-instances.bat to inspect their roles.
endlocal
