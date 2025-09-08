#!/bin/bash
# Build script for Revix Kotlin Library JAR (Linux/macOS)
# This script builds the shared Kotlin library as a JAR file

echo "🏗️  Building Revix Kotlin Library JAR..."
echo

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "❌ Error: gradlew not found. Please run this script from the root of the Revix repository."
    exit 1
fi

# Make gradlew executable if needed
chmod +x gradlew

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew :shared:clean

# Build the shared library JAR
echo "📦 Building Kotlin library JAR..."
./gradlew :shared:build :shared:jvmSourcesJar

# Check if build succeeded
if [ ! -f "shared/build/libs/shared-jvm-1.0.0.jar" ]; then
    echo "❌ Error: Build failed. JAR not found at shared/build/libs/shared-jvm-1.0.0.jar"
    exit 1
fi

echo "✅ JAR built successfully!"
echo
echo "📁 Generated files:"
echo "  - Main JAR: shared/build/libs/shared-jvm-1.0.0.jar"
if [ -f "shared/build/libs/shared-jvm-1.0.0-sources.jar" ]; then
    echo "  - Sources JAR: shared/build/libs/shared-jvm-1.0.0-sources.jar"
fi
echo
echo "📋 File information:"
jarsize=$(stat -c%s "shared/build/libs/shared-jvm-1.0.0.jar" 2>/dev/null || stat -f%z "shared/build/libs/shared-jvm-1.0.0.jar" 2>/dev/null)
echo "  Size: $jarsize bytes"
echo
echo "🎉 Build completed successfully!"
echo
echo "📖 Usage instructions:"
echo "  1. Copy the JAR file to your project's libs folder"
echo "  2. Add to your build.gradle.kts:"
echo "     dependencies { implementation(files(\"libs/shared-jvm-1.0.0.jar\")) }"
echo "  3. See README-JAR.md for detailed integration instructions"
echo