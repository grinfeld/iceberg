# Spark Iceberg Example

This project demonstrates how to use Apache Spark with Apache Iceberg for table management and data processing.

## Prerequisites

- Java 8 or later
- Scala 2.12
- SBT (Scala Build Tool)
- Docker and Docker Compose (for MinIO)

## Project Structure

```
.
├── build.sbt              # Project configuration and dependencies
├── project/
│   └── plugins.sbt        # SBT plugins
├── docker-compose.yml     # MinIO configuration
├── scripts/
│   └── setup-minio.sh     # MinIO setup script
└── src/
    └── main/
        └── scala/
            └── com/
                └── example/
                    └── IcebergExample.scala  # Example code
```

## Dependencies

- Apache Spark 3.5.0
- Apache Iceberg 1.4.3
- Apache Hadoop 3.3.6
- AWS SDK 2.24.17
- DynamicYield event-collection-schema 7.2.0

## Setting up MinIO

1. Start MinIO:
```bash
docker-compose up -d
```

2. Run the setup script:
```bash
chmod +x scripts/setup-minio.sh
./scripts/setup-minio.sh
```

3. Access MinIO Console:
- URL: http://localhost:9001
- Username: minioadmin
- Password: minioadmin

## Running the Example

1. Build the project:
```bash
sbt clean assembly
```

2. Run the example:
```bash
sbt run
```

## Features Demonstrated

- Creating an Iceberg table
- Writing data to an Iceberg table
- Reading data from an Iceberg table
- Time travel capabilities (commented out in the example)
- S3/MinIO integration

## Configuration

The project is configured to use Iceberg with Spark SQL and MinIO. The necessary configurations are set in `build.sbt`:

- Iceberg Spark Session Extensions
- Spark Catalog configuration
- MinIO endpoint and credentials
- S3A filesystem configuration

## Notes

- The example uses a local Spark instance for demonstration
- MinIO is used as an S3-compatible storage backend
- For production use, configure appropriate storage locations and security settings 