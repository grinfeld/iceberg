# Intro

I wanted to play a little bit with Iceberg. I had some expectations of how it should work, based on some internet readings.

I created a Scala project to read/write an Iceberg table.

Theoretically, my code supports 3 types of catalog: hadoop, hive and glue. I was experimenting with hadoop (hive works, too) type.

After it worked, I found that despite having metadata, the table files were written to Object Storage (minIO) partitioned by the partition keys I had chosen
and files are were small (e.g. s3a://bucket/key1=xxxx/key1=xxxx/...). 
I expected files to be written in the root, and partitions (like the rest metadata) will be stored in Avro files under the metadata folder.
So I tried to add `write.location-provider.impl` that writes files under a different path. For example, 
if we have partitions: `key1` and `key2`, we can define that all files will be written only under `key1` path, e.g., `s3a://bucket/key1=xxxx/` 
and metadata defined by other keys stored in catalog files. It didn't solve the issue with small files, but now they are under the same key in Object Storage.
I added compaction (by calling [rewrite_data_files](https://iceberg.apache.org/docs/latest/spark-procedures/#rewrite_data_files) immediately after the main job had finished. 
However, it looks that despite storing files in flat structure, it compacts according to partitionBy fields :(    

Main entry is [IcebergApp](src/main/scala/com/mikerusoft/examples/IcebergApp.scala)

Below, I've included a short summary and explanation, created by Claude. :) 

## Key Features

### 1. **Custom Location Provider (FlatLocationProvider)**
- Implements a flexible partition management system
- Supports selective partition field filtering through configuration
- Allows dynamic control over which partition fields are used when writing data
- Enables flat directory structures or partial partitioning schemes
- Supports all Iceberg partition transforms (identity, bucket, truncate, year, month, day, hour)

### 2. **Iceberg Operations**
- Create and manage Iceberg catalogs programmatically
- Create tables with custom schemas and partition specifications
- Write data to Iceberg tables with various partitioning strategies
- Read and query Iceberg tables using Spark SQL
- Support for schema evolution and table management

### 3. **AWS Integration**
- S3 integration for reading source data
- AWS Glue catalog support
- Configurable AWS credentials and endpoints
- Support for MinIO and other S3-compatible storage

### 4. **Flexible Configuration**
- Profile-based configuration system (local, docker, hadoop profiles)
- Support for multiple data sources and destinations
- Configurable partition strategies via configuration files
- Easy switching between different deployment environments

## Technologies Used

- **Apache Spark**: 4.0.0
- **Apache Iceberg**: 1.10.0
- **Scala**: 2.13.16
- **Java**: 17
- **Hadoop**: 3.4.1
- **AWS SDK**: 2.33.11 (S3, STS, Glue)
- **Apache Hive**: 3.1.3 (Metastore)
- **Apache Parquet**: 1.13.1

## Project Structure

```
iceberg/
├── src/main/scala/com/mikerusoft/examples/
│   ├── IcebergApp.scala              # Main application entry point
│   ├── SimpleIcebergApp.scala        # Simplified Iceberg example
│   ├── S3IcebergExample.scala        # S3-specific example
│   ├── FlatLocationProvider.scala    # Custom location provider implementation
│   ├── config/
│   │   ├── ConfigParser.scala        # Configuration parsing utilities
│   │   └── IcebergConf.scala        # Iceberg configuration wrapper
│   └── tools/
│       ├── MySparkImplicits.scala   # Spark DataFrame extensions
│       ├── PartitionBy.scala        # Partition specification helpers
│       └── Profiles.scala           # Configuration profiles
├── src/main/resources/
│   └── application.hadoop.conf      # Hadoop/AWS configuration
├── docker-compose.yml               # Docker setup for local testing
└── build.sbt                        # SBT build configuration
```

## How FlatLocationProvider Works

The `FlatLocationProvider` is a custom Iceberg location provider that allows you to control partition directory structure through configuration:

1. **Configuration**: Set `write.location-provider.flat.fields` to specify which partition fields to use
2. **Filtering**: It filters partition fields based on the configured field names
3. **Partial Partitioning**: Creates a new PartitionSpec with only the selected fields
4. **Data Writing**: Writes data using the filtered partition structure

### Example Configuration
```conf
write.location-provider.impl = "com.mikerusoft.examples.FlatLocationProvider"
write.location-provider.flat.fields = "region,date"  # Use only these partition fields
```

## Building the Project

### Prerequisites
- Java 17 or higher
- SBT (Scala Build Tool)
- Docker (optional, for local testing)

### Compile
```bash
sbt compile
```

### Run Tests
```bash
sbt test
```

### Package
```bash
sbt package
```

## Running the Application

### Local Mode
```bash
sbt "runMain com.mikerusoft.examples.IcebergApp local"
```

### Docker Mode
```bash
# Start Docker services
docker-compose up -d

# Run application
sbt "runMain com.mikerusoft.examples.IcebergApp docker"
```

### Hadoop/Production Mode
```bash
sbt "runMain com.mikerusoft.examples.IcebergApp hadoop"
```

## Configuration Profiles

The project supports multiple configuration profiles:

- **local**: Uses local file system and in-memory catalog
- **docker**: Uses MinIO (S3-compatible) and containerized services
- **hadoop**: Production configuration with AWS S3 and Glue catalog

Configuration files are located in `src/main/resources/` and follow the pattern `application.{profile}.conf`.

## Use Cases

1. **Data Lake Migration**: Migrate Parquet data to Iceberg format with custom partitioning
2. **Partition Strategy Optimization**: Experiment with different partitioning schemes without restructuring data
3. **Multi-Region Data Management**: Control partition structure for optimal query performance
4. **Schema Evolution**: Demonstrate Iceberg's schema evolution capabilities
5. **Cloud Integration**: Examples of integrating Spark with AWS S3 and Glue

## Key Classes

- **FlatLocationProvider**: Custom location provider for flexible partition management
- **MySparkImplicits**: Extension methods for Spark DataFrames (catalog and table creation)
- **PartitionBy**: Utilities for building Iceberg partition specifications
- **IcebergConf**: Configuration wrapper for Iceberg table settings
- **ConfigParser**: Profile-based configuration management
