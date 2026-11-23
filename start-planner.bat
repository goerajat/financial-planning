@echo off
REM Stop any running planner first, then build the web jar (React frontend
REM included) and start Spring Boot on http://0.0.0.0:8080. The `&&` between
REM package and java means a build failure won't launch the server, and since
REM the previous planner was killed up front you won't be served a stale jar.
REM
REM   start-planner.bat              build, then start
REM   start-planner.bat --no-build   start the jar already in target\
REM   start-planner.bat --no-browser do not open a browser tab
REM
REM The build and server run in a HIDDEN console; everything they print goes
REM to %USERPROFILE%\.financial-planning\planner.log. A start that never
REM answers /api/plan/default-rates prints the tail of that log here.
REM
REM Launched through WMI, not Start-Process: a child started directly stays in
REM its caller's job object, so a server started from a Claude Code session or
REM a scheduled task would die with it. WMI creates the process outside the
REM caller's tree, with the user's registry environment.
REM
REM Call it by full path: with NoDefaultCurrentDirectoryInExePath set (Claude
REM Code's shell, Task Scheduler) cmd will not resolve a .bat from the current
REM directory. Sleeps are `ping` rather than `timeout`, which exits at once
REM when stdin is not a console.
cd /d "%~dp0"
call "%~dp0stop-planner.bat"

set URL=http://127.0.0.1:8080
set LOG=%USERPROFILE%\.financial-planning\planner.log
set JAR=target\financial-planning-1.0-SNAPSHOT-web.jar
if not exist "%USERPROFILE%\.financial-planning" mkdir "%USERPROFILE%\.financial-planning"

set BUILD=1
set BROWSER=1
for %%a in (%*) do (
  if "%%a"=="--no-build" set BUILD=
  if "%%a"=="--no-browser" set BROWSER=
)
if not defined BUILD if not exist "%JAR%" (
  echo %JAR% not found; building anyway.
  set BUILD=1
)

echo Starting planner, log at %LOG%
if defined BUILD (
  powershell -NoProfile -Command "$si = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}; $r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine='cmd.exe /c (mvn -q package -DskipTests && java -jar %JAR%) > %LOG% 2>&1'; CurrentDirectory='%~dp0'; ProcessStartupInformation=$si}; if ($r.ReturnValue -ne 0) { Write-Host ('WMI launch failed: ' + $r.ReturnValue); exit 1 }"
  set MAXTRIES=200
) else (
  powershell -NoProfile -Command "$si = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}; $r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine='cmd.exe /c java -jar %JAR% > %LOG% 2>&1'; CurrentDirectory='%~dp0'; ProcessStartupInformation=$si}; if ($r.ReturnValue -ne 0) { Write-Host ('WMI launch failed: ' + $r.ReturnValue); exit 1 }"
  set MAXTRIES=30
)

echo Waiting for planner at %URL% ...
set tries=0
:wait_loop
set /a tries+=1
if %tries% GTR %MAXTRIES% (
  echo Timed out waiting for the planner. Last lines of %LOG%:
  powershell -NoProfile -Command "Get-Content -Tail 40 '%LOG%'"
  goto :eof
)
ping -n 3 127.0.0.1 >nul
curl -s -o nul -m 1 %URL%/api/plan/default-rates
if errorlevel 1 goto wait_loop
echo Planner is up at %URL% (tailnet: http://%COMPUTERNAME%:8080).

if not defined BROWSER goto :eof
if defined SSH_CONNECTION goto :eof

if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
  start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" "%URL%"
) else if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" (
  start "" "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" "%URL%"
) else if exist "%LocalAppData%\Google\Chrome\Application\chrome.exe" (
  start "" "%LocalAppData%\Google\Chrome\Application\chrome.exe" "%URL%"
) else (
  start "" "%URL%"
)
