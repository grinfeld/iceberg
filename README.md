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
And it sounds reasonable since different spark tasks, possibly with different executors write data. 
I added compaction (by calling [rewrite_data_files](https://iceberg.apache.org/docs/latest/spark-procedures/#rewrite_data_files) immediately after the main job had finished. 
However, it looks that despite storing files in flat structure, it compacts according to original partitionBy fields :(  

Main entry is [IcebergApp](src/main/scala/com/mikerusoft/examples/IcebergApp.scala)

Below, I've included a short summary and explanation, created by Claude (and changed partially by me) :) 

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

## Project Structure

```
iceberg/
├── src/main/scala/com/mikerusoft/examples/
│   ├── IcebergApp.scala                    # Main application entry point
│   ├── SimpleIcebergApp.scala              # Simplified Iceberg example
│   ├── S3IcebergExample.scala              # S3-specific example
│   ├── FlatLocationProvider.scala          # Custom location provider implementation
│   ├── compaction/                         # package defines the compaction process
│   │   ├── Strategy.scala                  # strategy parameter for compaction job
│   │   ├── OptionsField.scala              # defines the options parameter to use with the Strategy.scala
│   │   ├── RewriteJobOrderValues.scala     # defines the values for Option field re-write-job-order  
│   ├── config/
│   │   ├── ConfigParser.scala              # Configuration parsing utilities
│   │   └── IcebergConf.scala               # Iceberg configuration wrapper
│   └── tools/
│       ├── MySparkImplicits.scala          # Spark DataFrame extensions
│       ├── PartitionBy.scala               # Partition specification helpers
│       └── Profiles.scala                  # Configuration profiles
├── src/main/resources/
│   └── application.hadoop.conf             # Hadoop/AWS configuration
├── docker-compose.yml                      # Docker setup for local testing
└── build.sbt                               # SBT build configuration
```

## How FlatLocationProvider Works

The `FlatLocationProvider` is a custom Iceberg location provider that allows you to control partition directory structure through configuration:

1. **Configuration**: Set `write.location-provider.flat.fields` table property to specify which partition fields to use
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

## Running the Application

### Docker
```bash
# Start Docker services contained minIO, hive metastore
docker-compose up -d
```

## Run (locally via IDE)

The entry point is [IcebergApp](src/main/scala/com/mikerusoft/examples/IcebergApp.scala). While running it, please set env variable `PROFILE` - it should be one of `hadoop`, `hive`, `glue`.

## Configuration Profiles

The project supports multiple configuration profiles:

The initial configurations is stored in [application.conf](src/main/resources/application.conf)
The additional configuration is loaded by value received from env var `PROFILE`, i.e. `application.{profile}.conf` located in [src/main/resources/](src/main/resources/).
Possible values hadoop, hive or glue.

## Code explanation

```scala
    val config = profile.createConfig()
    val spark: SparkSession = config.sparkSession()
    val icebergConf: IcebergConf = config.icebergConf()
    
    spark.read.parquet(config.getString("app.s3.from"))
        .createCatalogIfDoesNotExist(icebergConf)
        .createTableOfDoesNotExist(icebergConf, config.getPartitionBy)
        .withColumn("ts", timestamp_millis(col("resolvedTimestamp")))
        .writeTo(icebergConf.fullTableName)
        .appendAnd()
      .compact(config.icebergConf(), Binpack(MinInputFiles(1)))
```

* `createCatalogIfDoesNotExist` -> creates catalog if not exists
* *  Catalog name is taken from spark.sql.catalog configuration (`mycatalog` value is used in example)
```hocon
app {
  config {
    spark {
      sql {
        catalog {
          mycatalog {
            type = "hadoop"
            ........
            ........
            ........
          }
        }
      }
    }
  }
}
```
* `createTableOfDoesNotExist` -> creates database if not exists with schema based on raw parquet data
* * In Iceberg the table is in format `database.table` 
```hocon
app {
  iceberg {
    database = "demo"
    database = ${?ICEBERG_DB_NAME}
    table = "raw_events"
    table = ${?ICEBERG_TABLE_NAME}
  }
}
```
* * Partitions is defined by set of fields in [PartitionBy](src/main/scala/com/mikerusoft/examples/tools/PartitionBy.scala) or/and in configuration:
```hocon
app {
  partition-by = [
    # supported types are yhe same as iceberg:
    # 1. identity -> just col name expected
    # 2. bucket -> col name and bucket size
    # 3. date -> col name and date func, a.k.a. year, month, day or hour
    # 4. truncate -> col name and truncate value (see iceberg manaul: https://iceberg.apache.org/spec/#truncate-transform-details)
    {
      col = "sectionId"
      type = "identity"
    },
    {
      col = "dyid"
      type = "bucket"
      size = 16
      size = ${?DYID_BUCKET_SIZE}
    },
    {
      col = "eventType"
      type = "identity"
    },
    {
      col = "ts"
      type = "date"
      func = "hour"
    }
  ]
}
```



