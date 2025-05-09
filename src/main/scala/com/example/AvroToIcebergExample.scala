package com.example

import com.typesafe.config.Config
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.avro.functions.from_avro
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.avro.SchemaConverters
import com.dy.rawV2.gen.RawEventV2

object AvroToIcebergExample {
  def main(args: Array[String]): Unit = {
    val profile = System.getProperty("PROFILE")

    val parser = ConfigParser(profile)

    // Load configuration
    val config = parser.config

    val appConfig: Config = config.getConfig("app")

    // Create Spark session with configurations
    var sparkBuilder: SparkSession.Builder = SparkSession.builder()
      .appName(appConfig.getString("name"))

    if(appConfig.hasPath("master"))
      sparkBuilder = sparkBuilder.master(appConfig.getString("master"))

    sparkBuilder = parser.applyConfig("app.config", sparkBuilder)

    val spark = sparkBuilder.getOrCreate()

    val avroData = spark.read.format("avro").load(appConfig.getString("s3.from"))

    // Convert to RawEventV2 format
    val rawEvents = avroData.select(
      from_avro(col("value"), RawEventV2.getClassSchema.toString).as("raw_event")
    ).select("raw_event.*")

    // Get Avro schema and convert to Spark SQL schema using built-in converter
    val avroSchema = RawEventV2.getClassSchema
    val sparkSchema = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[org.apache.spark.sql.types.StructType]

    // Create database if it doesn't exist
    spark.sql(s"CREATE DATABASE IF NOT EXISTS ${appConfig.getString("iceberg.database")}")

    // Check if table exists
    val tableExists = spark.catalog.tableExists(appConfig.getString("iceberg.database"), appConfig.getString("iceberg.table"))

    if (!tableExists) {
      // Create table with initial schema
      val tableLocation = s"${appConfig.getString("iceberg.warehouse")}/${appConfig.getString("iceberg.database")}/${appConfig.getString("iceberg.table")}"
      val createTableSQL = s"""
        CREATE TABLE ${appConfig.getString("iceberg.database")}.${appConfig.getString("iceberg.table")} (
          ${sparkSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n          ")}
        ) USING iceberg
      """
      
      println("Creating new table with schema:")
      println(createTableSQL)
      
      spark.sql(createTableSQL)
    } else {
      // Table exists, we'll let Iceberg handle schema evolution automatically
      println("Table already exists. Iceberg will handle schema evolution automatically.")
    }

    // Write to Iceberg table - Iceberg will automatically handle schema evolution
    rawEvents.writeTo(s"${appConfig.getString("iceberg.database")}.${appConfig.getString("iceberg.table")}")
      .append()

    // Print current schema and data
    println("\nCurrent table schema:")
    spark.table(s"${appConfig.getString("iceberg.database")}.${appConfig.getString("iceberg.table")}").printSchema()
    
    println("\nData in Iceberg table:")
    spark.table(s"${appConfig.getString("iceberg.database")}.${appConfig.getString("iceberg.table")}")
      .show(5, truncate = false)

    spark.stop()
  }
} 