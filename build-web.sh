#!/bin/bash
# Build script for Financial Planner Web Application (Unix/Mac)
# Creates a self-contained uberjar with React frontend and Spring Boot backend

set -e

echo "============================================"
echo " Financial Planner - Web Build Script"
echo "============================================"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    exit 1
fi

echo "[1/4] Cleaning previous build..."
mvn clean -q

echo "[2/4] Building React frontend..."
echo "      (This may take a few minutes on first run)"
mvn generate-resources -q

echo "[3/4] Running tests..."
mvn test -q -Dskip.frontend=true

echo "[4/4] Packaging uberjar..."
mvn package -DskipTests -Dskip.frontend=true -q

echo
echo "============================================"
echo " Build successful!"
echo "============================================"
echo
echo "Output: target/financial-planning-1.0-SNAPSHOT-web.jar"
echo
echo "Run with: java -jar target/financial-planning-1.0-SNAPSHOT-web.jar"
echo "Then open: http://localhost:8080"
echo
