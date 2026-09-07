@echo off
setlocal EnableDelayedExpansion

set "COUNT=%~1"
set "BASE_PORT=%~2"

if "%COUNT%"=="" set "COUNT=3"
if "%BASE_PORT%"=="" set "BASE_PORT=8080"

for /L %%N in (1,1,%COUNT%) do (
    set /A PORT=BASE_PORT+%%N-1
    echo ===== http://localhost:!PORT!/api/leader =====
    curl.exe --silent --show-error --fail "http://localhost:!PORT!/api/leader"
    if errorlevel 1 echo Instance on port !PORT! is not reachable.
    echo.
)

endlocal
