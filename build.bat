@echo off
REM Build script for Revix Docker deployment (Windows)
REM This script ensures the application is built before creating the Docker image

echo 🏗️  Building Revix application...

REM Check if we're in the right directory
if not exist "gradlew.bat" (
    echo ❌ Error: gradlew.bat not found. Please run this script from the root of the Revix repository.
    exit /b 1
)

REM Build the application distribution
echo 📦 Building application distribution...
call gradlew.bat server:installDist

REM Check if build succeeded
if not exist "server\build\install\server" (
    echo ❌ Error: Build failed. Distribution not found at server\build\install\server
    exit /b 1
)

echo ✅ Application built successfully

REM Build Docker image
echo 🐳 Building Docker image...
cd deploy\docker
docker compose build server

echo 🎉 Build completed successfully!
echo.
echo To start the application:
echo   cd deploy\docker
echo   docker compose up -d
echo.
echo To verify the installation:
echo   curl http://localhost:8080/health