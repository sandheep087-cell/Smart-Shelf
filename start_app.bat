@echo off
echo Starting Smart Shelf Application...

echo Starting Backend...
cd /d "%~dp0smart-shelf-backend"
start "Smart Shelf Backend" cmd /k "mvn spring-boot:run"

echo Opening Application in browser...
timeout /t 5 /nobreak >nul
start "" "http://localhost:8080"

echo Application launched on http://localhost:8080!
