#!/bin/bash

# Build script for Revix Docker deployment
# This script ensures the application is built before creating the Docker image

set -e

echo "🏗️  Building Revix application..."

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "❌ Error: gradlew not found. Please run this script from the root of the Revix repository."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Build the application distribution
echo "📦 Building application distribution..."
./gradlew server:installDist

# Check if build succeeded
if [ ! -d "server/build/install/server" ]; then
    echo "❌ Error: Build failed. Distribution not found at server/build/install/server"
    exit 1
fi

echo "✅ Application built successfully"

# Build Docker image using docker compose
echo "🐳 Building Docker image..."
cd deploy/docker

# Clean up any existing containers
docker compose down 2>/dev/null || true

# Build the image
docker compose build server

echo "🎉 Build completed successfully!"
echo ""
echo "To start the application:"
echo "  cd deploy/docker"
echo "  docker compose up -d"
echo ""
echo "To verify the installation:"
echo "  curl http://localhost:8080/health"