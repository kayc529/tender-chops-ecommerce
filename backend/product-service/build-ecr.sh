#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: ./build-ecr.sh <service-name> <aws-account-id> <aws-region>"
  echo "Example: ./build-ecr.sh order-service 123456789012 ap-southeast-1"
  exit 1
fi

SERVICE_NAME="$1"
AWS_ACCOUNT_ID="$2"
AWS_REGION="$3"
IMAGE_TAG="latest"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ECR_REPOSITORY_URI="${ECR_REGISTRY}/tender-chops-dev/${SERVICE_NAME}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}" && pwd)"

cd "${PROJECT_ROOT}"

if [ ! -f "Dockerfile" ]; then
  echo "Error: Dockerfile not found in ${PROJECT_ROOT}"
  exit 1
fi

echo "Logging in to Amazon ECR..."
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

echo "Building image ${SERVICE_NAME}:${IMAGE_TAG} for linux/amd64..."
docker build --platform linux/amd64 -t "${SERVICE_NAME}:${IMAGE_TAG}" .

echo "Tagging image as ${ECR_REPOSITORY_URI}:${IMAGE_TAG}..."
docker tag "${SERVICE_NAME}:${IMAGE_TAG}" "${ECR_REPOSITORY_URI}:${IMAGE_TAG}"

echo "Pushing image to ECR..."
docker push "${ECR_REPOSITORY_URI}:${IMAGE_TAG}"

echo "Done. Image pushed to ${ECR_REPOSITORY_URI}:${IMAGE_TAG}"