#!/bin/bash

# Default tag to 1.0.0 if not provided
TAG=${1:-1.0.0}
IMAGE_NAME="ghcr.io/carljmosca/qwatcher:$TAG"

echo "----------------------------------------"
echo "Building QWatcher Container Image"
echo "Tag: $TAG"
echo "Image: $IMAGE_NAME"
echo "----------------------------------------"

# Ensure Maven build artifacts exist
if [ ! -d "target/quarkus-app" ]; then
    echo "Artifacts not found in target/quarkus-app."
    echo "Running Maven build..."
    if command -v mvn &> /dev/null; then
        mvn clean package -DskipTests
        if [ $? -ne 0 ]; then
            echo "Maven build failed."
            exit 1
        fi
    else
        echo "Error: 'mvn' not found. Please run './mvnw package' first."
        exit 1
    fi
else
    echo "Using existing artifacts in target/quarkus-app."
    echo "(Run 'mvn clean package' manually if you need to rebuild the JAR)"
fi

# Build Container Image
if command -v podman &> /dev/null; then
    echo "Building with Podman..."
    podman build -f src/main/docker/Dockerfile.jvm -t "$IMAGE_NAME" .
elif command -v docker &> /dev/null; then
    echo "Building with Docker..."
    docker build -f src/main/docker/Dockerfile.jvm -t "$IMAGE_NAME" .
else
    echo "Error: Neither podman nor docker found."
    exit 1
fi

echo "----------------------------------------"
echo "Build Complete!"
echo "Run './src/main/deploy/deploy.sh' to install the service."
echo "----------------------------------------"
