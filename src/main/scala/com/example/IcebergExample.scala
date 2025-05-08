package com.example

import org.apache.spark.sql.SparkSession

object IcebergExample {
  def main(args: Array[String]): Unit = {
    // Create Spark session with Iceberg support
    val spark = SparkSession.builder()
      .appName("Spark Iceberg Example")
      .master("local[*]")
      .getOrCreate()

    try {
      // Create a sample DataFrame
      val data = Seq(
        (1, "Alice", 25),
        (2, "Bob", 30),
        (3, "Charlie", 35)
      )
      val df = spark.createDataFrame(data).toDF("id", "name", "age")

      // Create an Iceberg table
      spark.sql("CREATE DATABASE IF NOT EXISTS demo")
      spark.sql("CREATE TABLE IF NOT EXISTS demo.users (id INT, name STRING, age INT) USING iceberg")

      // Write data to the Iceberg table
      df.writeTo("demo.users").append()

      // Read data from the Iceberg table
      val result = spark.table("demo.users")
      println("Data from Iceberg table:")
      result.show()

      // Demonstrate time travel (if you have multiple snapshots)
      // spark.table("demo.users").asOf("2024-01-01 00:00:00").show()

    } finally {
      spark.stop()
    }
  }
} 