package com.example

import com.dynamicyield.eventcollection.schema.RawEventV2
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.avro.functions.from_avro
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.avro.SchemaConverters

object AvroToIcebergExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Avro to Iceberg Conversion")
      .master("local[*]")
      .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
      .config("spark.hadoop.fs.s3a.access.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
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

      // Create Iceberg table
      spark.sql("CREATE DATABASE IF NOT EXISTS demo")

      // Get Avro schema and convert to Spark SQL schema using built-in converter
      val avroSchema = RawEventV2.getClassSchema
      val sparkSchema = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[org.apache.spark.sql.types.StructType]
      
      // Create table with dynamic schema
      val createTableSQL = s"""
        CREATE TABLE IF NOT EXISTS demo.raw_events (
          ${sparkSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n          ")}
        ) USING iceberg
        LOCATION 's3a://iceberg/raw_events'
      """
      
      println("Creating table with schema:")
      println(createTableSQL)
      
      spark.sql(createTableSQL)

      // Write to Iceberg table
      rawEvents.writeTo("demo.raw_events").append()

      // Verify the data
      println("Data in Iceberg table:")
      spark.table("demo.raw_events").show(5)

    } finally {
      spark.stop()
    }
  }
} 