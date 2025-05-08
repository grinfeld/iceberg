#!/bin/bash

# Wait for MinIO to be ready
echo "Waiting for MinIO to be ready..."
until curl -s http://localhost:9000/minio/health/live > /dev/null; do
    sleep 1
done

# Wait for Hive Metastore to be ready
echo "Waiting for Hive Metastore to be ready..."
until nc -z localhost 9083; do
    sleep 1
done

# Install MinIO client if not installed
if ! command -v mc &> /dev/null; then
    echo "Installing MinIO client..."
    wget https://dl.min.io/client/mc/release/linux-amd64/mc
    chmod +x mc
    sudo mv mc /usr/local/bin/
fi

# Configure MinIO client
mc alias set myminio http://localhost:9000 minioadmin minioadmin

# Create bucket for Iceberg
mc mb myminio/iceberg

# Set bucket policy to allow all operations (for development only)
mc policy set download myminio/iceberg
mc policy set upload myminio/iceberg

echo "Setup completed!"
echo "MinIO Console: http://localhost:9001"
echo "Login with:"
echo "Username: minioadmin"
echo "Password: minioadmin"
echo ""
echo "Hive Metastore is running on port 9083" 