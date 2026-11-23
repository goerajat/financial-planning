@echo off
REM Stop the planner's Spring Boot server: kill whatever listens on :8080.
powershell -NoProfile -Command "$c = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -First 1; if ($c) { Stop-Process -Id $c.OwningProcess -Force; Write-Host ('Stopped planner (PID ' + $c.OwningProcess + ')') } else { Write-Host 'No planner running on :8080' }"
