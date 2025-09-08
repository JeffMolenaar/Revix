@echo off
REM Build script for Revix Kotlin Library JAR (Windows)
REM This script builds the shared Kotlin library as a JAR file

echo 🏗️  Building Revix Kotlin Library JAR...
echo.

REM Check if we're in the right directory
if not exist "gradlew.bat" (
    echo ❌ Error: gradlew.bat not found. Please run this script from the root of the Revix repository.
    pause
    exit /b 1
)

REM Clean previous builds
echo 🧹 Cleaning previous builds...
call gradlew.bat :shared:clean

REM Build the shared library JAR
echo 📦 Building Kotlin library JAR...
call gradlew.bat :shared:build :shared:jvmSourcesJar

REM Check if build succeeded
if not exist "shared\build\libs\shared-jvm-1.0.0.jar" (
    echo ❌ Error: Build failed. JAR not found at shared\build\libs\shared-jvm-1.0.0.jar
    pause
    exit /b 1
)

echo ✅ JAR built successfully!
echo.
echo 📁 Generated files:
echo   - Main JAR: shared\build\libs\shared-jvm-1.0.0.jar
if exist "shared\build\libs\shared-jvm-1.0.0-sources.jar" (
    echo   - Sources JAR: shared\build\libs\shared-jvm-1.0.0-sources.jar
)
echo.
echo 📋 File information:
for %%F in ("shared\build\libs\shared-jvm-1.0.0.jar") do echo   Size: %%~zF bytes
echo.
echo 🎉 Build completed successfully!
echo.
echo 📖 Usage instructions:
echo   1. Copy the JAR file to your project's libs folder
echo   2. Add to your build.gradle.kts:
echo      dependencies { implementation(files("libs/shared-jvm-1.0.0.jar")) }
echo   3. See README-JAR.md for detailed integration instructions
echo.
pause