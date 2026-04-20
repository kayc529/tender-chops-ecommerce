#!/bin/bash
set -e

SERVICES=("cart-service" "inventory-service" "order-service" "payment-service" "product-service")

for service in "${SERVICES[@]}"; do
    echo "=============================="
    echo "Building $service..."
    echo "=============================="

    (cd "backend/$service" && ./build.sh) || {
        echo "Failed to build $service"
        exit 1
    }
done

echo "All services built successfully."