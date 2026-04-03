#!/usr/bin/env bash
set -e

SERVICE_NAME="cart-service"
IMAGE_NAME="cart-service:local"

echo ""
echo "========================================="
echo "   Building ${SERVICE_NAME} JAR"
echo "========================================="
echo ""

mvn clean package -DskipTests

echo ""
echo "========================================="
echo "   Building Docker Image: ${IMAGE_NAME}"
echo "========================================="
echo ""

docker build -t ${IMAGE_NAME} .
