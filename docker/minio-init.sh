#!/bin/bash

# init minIO

sleep 10
mc alias set myminio "$STORAGE_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

mc mb myminio/test
mc mb myminio/parquet
mc mb myminio/iceberg-demo

mc anonymous set public myminio/test
mc anonymous set public myminio/parquet
mc anonymous set public myminio/iceberg-demo

mc cp --recursive /examples/parquet/ myminio/parquet/
echo 'MinIO setup completed'