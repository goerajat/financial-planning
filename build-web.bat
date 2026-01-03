@echo off
REM Build script for Financial Planner Web Application (Windows)
REM Creates a self-contained uberjar with React frontend and Spring Boot backend

echo ============================================
echo  Financial Planner - Web Build Script
echo ============================================
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    exit /b 1
)

echo [1/4] Cleaning previous build...
call mvn clean -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven clean failed
    exit /b 1
)

echo [2/4] Building React frontend...
echo       (This may take a few minutes on first run)
call mvn generate-resources -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Frontend build failed
    exit /b 1
)

echo [3/4] Running tests...
call mvn test -q -Dskip.frontend=true
if %ERRORLEVEL% neq 0 (
    echo ERROR: Tests failed
    exit /b 1
)

echo [4/4] Packaging uberjar...
call mvn package -DskipTests -Dskip.frontend=true -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Packaging failed
    exit /b 1
)

echo.
echo ============================================
echo  Build successful!
echo ============================================
echo.
echo Output: target\financial-planning-1.0-SNAPSHOT-web.jar
echo.
echo Run with: java -jar target\financial-planning-1.0-SNAPSHOT-web.jar
echo Then open: http://localhost:8080
echo.
