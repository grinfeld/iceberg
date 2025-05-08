package com.example

import org.apache.spark.sql.SparkSession

object S3IcebergExample {
  def main(args: Array[String]): Unit = {
    // Create Spark session with Iceberg and S3/MinIO support
    val spark = SparkSession.builder()
      .appName("Spark Iceberg S3 Example")
      .master("local[*]")
      .config("spark.hadoop.fs.s3a.access.key", "minioadmin")  // Default MinIO credentials
      .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
      .getOrCreate()

    val df = spark.read.format("avro").load("s3a://iceberg-demo/users").as[RawEventV2]
    try {
      // Create a sample DataFrame
      val data = Seq(
        (1, "Alice", 25),
        (2, "Bob", 30),
        (3, "Charlie", 35)
      )
      val df = spark.createDataFrame(data).toDF("id", "name", "age")

      // Create an Iceberg table in S3/MinIO
      spark.sql("CREATE DATABASE IF NOT EXISTS demo")
      spark.sql(
        """
        CREATE TABLE IF NOT EXISTS demo.users (
          id INT,
          name STRING,
          age INT
        ) USING iceberg
        LOCATION 's3a://iceberg-demo/users'
        """
      )

      // Write data to the Iceberg table
      df.writeTo("demo.users").append()

      // Read data from the Iceberg table
      val result = spark.table("demo.users")
      println("Data from Iceberg table in S3/MinIO:")
      result.show()

      // Example of time travel
      // spark.table("demo.users").asOf("2024-01-01 00:00:00").show()

      // Example of reading specific partitions
      // spark.table("demo.users").where("age > 30").show()

    } finally {
      spark.stop()
    }
  }
} 