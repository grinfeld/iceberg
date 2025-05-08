package com.example

import com.dynamicyield.eventcollection.schema.RawEventV2
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.avro.functions.from_avro
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.avro.SchemaConverters

object AvroToIcebergExample {
  def main(args: Array[String]): Unit = {
    // Parse environment from args or use default
    val environment = args.headOption.getOrElse("local")
    println(s"Running in $environment environment")

    val spark = SparkSession.builder()
      .appName(s"Avro to Iceberg Conversion - $environment")
      .master("local[*]")
      .apply {
        if (environment == "local") {
          // Local configuration with Hive and MinIO
          _
            .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
            .config("spark.hadoop.fs.s3a.access.key", "minioadmin")
            .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
            .config("spark.hadoop.fs.s3a.path.style.access", "true")
            // Hive catalog for local development
            .config("spark.sql.catalog.spark_catalog.catalog-impl", "org.apache.iceberg.hive.HiveCatalog")
            .config("spark.sql.catalog.spark_catalog.uri", "thrift://localhost:9083")
            .config("spark.sql.catalog.spark_catalog.clients", "1")
            .config("spark.sql.catalog.spark_catalog.warehouse", "s3a://iceberg/warehouse")
            .config("spark.sql.catalog.spark_catalog.s3.endpoint", "http://localhost:9000")
            .config("spark.sql.catalog.spark_catalog.s3.access-key", "minioadmin")
            .config("spark.sql.catalog.spark_catalog.s3.secret-key", "minioadmin")
        } else {
          // Production configuration with Glue and S3
          _
            .config("spark.sql.catalog.spark_catalog.catalog-impl", "org.apache.iceberg.aws.glue.GlueCatalog")
            .config("spark.sql.catalog.spark_catalog.io-impl", "org.apache.iceberg.aws.s3.S3FileIO")
            .config("spark.sql.catalog.spark_catalog.warehouse", "s3a://your-warehouse-bucket/warehouse")
            // AWS configurations will be picked up from environment or IAM role
        }
      }
      .getOrCreate()

    try {
      // Read Avro data from S3
      val avroData = spark.read
        .format("avro")
        .load("s3a://your-bucket/your-avro-path/*.avro")

      // Convert to RawEventV2 format
      val rawEvents = avroData.select(
        from_avro(col("value"), RawEventV2.getClassSchema.toString).as("raw_event")
      ).select("raw_event.*")

      // Get Avro schema and convert to Spark SQL schema using built-in converter
      val avroSchema = RawEventV2.getClassSchema
      val sparkSchema = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[org.apache.spark.sql.types.StructType]
      
      // Create database if it doesn't exist
      spark.sql("CREATE DATABASE IF NOT EXISTS demo")

      // Check if table exists
      val tableExists = spark.catalog.tableExists("demo", "raw_events")
      
      if (!tableExists) {
        // Create table with initial schema
        val createTableSQL = s"""
          CREATE TABLE demo.raw_events (
            ${sparkSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n            ")}
          ) USING iceberg
          LOCATION 's3a://your-warehouse-bucket/warehouse/demo/raw_events'
        """
        
        println("Creating new table with schema:")
        println(createTableSQL)
        
        spark.sql(createTableSQL)
      } else {
        // Table exists, we'll let Iceberg handle schema evolution automatically
        println("Table already exists. Iceberg will handle schema evolution automatically.")
      }

      // Write to Iceberg table - Iceberg will automatically handle schema evolution
      rawEvents.writeTo("demo.raw_events").append()

      // Verify the data and schema
      println("\nCurrent table schema:")
      spark.table("demo.raw_events").printSchema()
      
      println("\nData in Iceberg table:")
      spark.table("demo.raw_events").show(5)

    } finally {
      spark.stop()
    }
  }
} 