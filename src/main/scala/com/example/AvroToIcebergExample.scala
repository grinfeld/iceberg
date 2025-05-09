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

    val iceberg = parser.icebergConf()

    if (config.getBoolean("app.create.table")) {
      createTable(spark, iceberg)
    }

    // Write to Iceberg table - Iceberg will automatically handle schema evolution
    rawEvents.writeTo(s"${iceberg.database}.${iceberg.table}").append()

    // Print current schema and data
    println("\nCurrent table schema:")
    spark.table(s"${iceberg.database}.${iceberg.table}").printSchema()
    
    println("\nData in Iceberg table:")
    spark.table(s"${iceberg.database}.${iceberg.table}").show(5, truncate = false)

    spark.stop()
  }

  private def createTable(spark: SparkSession, iceberg: IcebergConf): Unit = {
    // Get Avro schema and convert to Spark SQL schema using built-in converter
    // Check if table exists
    val tableExists = spark.catalog.tableExists(iceberg.database, iceberg.table)
    if (tableExists) {
      val avroSchema = RawEventV2.getClassSchema
      val sparkSchema = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[org.apache.spark.sql.types.StructType]
      // Create table with initial schema
      println("Creating new table with schema:")

      spark.sql(
        s"""
        CREATE TABLE ${iceberg.fullTableName} (
          ${sparkSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n\t\t")}
        ) USING iceberg
      """)
    }
  }
} 