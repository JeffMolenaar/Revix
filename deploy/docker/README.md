# Docker Deployment

## Quick Start

The easiest way to build and deploy Revix is to use the build script from the repository root:

```bash
# From the repository root
./build.sh
```

This script will:
1. Build the application using Gradle
2. Build the Docker image 
3. Provide instructions for starting the services

## Common Issue: "Application distribution not found"

If you run `docker compose build server` directly and get an error about "Application distribution not found", you need to build the application first:

```bash
# Quick fix - from repository root:
./gradlew server:installDist
cd deploy/docker
docker compose build server
```

Or use the build script which handles everything:
```bash
./build.sh
```

## Manual Build Process

If you prefer to run the steps manually:

```bash
# 1. Build the application (from repository root)
./gradlew server:installDist

# 2. Build the Docker image
cd deploy/docker
docker compose build server

# 3. Start the services
docker compose up -d
```

## Available Dockerfiles

- `Dockerfile.server` - Default Dockerfile that requires the application to be pre-built on the host
- `Dockerfile.server.multistage` - Alternative multistage build that builds everything inside Docker

## Troubleshooting

If you get an error about "Application distribution not found", it means you need to build the application first:

```bash
# Run this from the repository root
./gradlew server:installDist
```

Or use the build script which handles everything:

```bash
./build.sh
```