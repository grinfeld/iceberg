package com.mikerusoft.examples

import org.apache.spark.sql.SparkSession

object S3IcebergExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Spark Iceberg S3 Example")
      .master("local[*]")
      .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      .config("spark.sql.catalog.mycatalog", "org.apache.iceberg.spark.SparkCatalog")
      .config("spark.sql.catalog.mycatalog.type", "hadoop")
      .config("spark.sql.catalog.mycatalog.warehouse", "s3a://iceberg-demo/warehouse/")
      .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
      .config("spark.hadoop.fs.s3a.access.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.secret.key", "minioadmin")
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.ssl.enabled", "false")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .getOrCreate()

    try {
      // Create a sample DataFrame
      val data = Seq(
        (1, "Alice", 25),
        (2, "Bob", 30),
        (3, "Charlie", 35)
      )
      val df = spark.createDataFrame(data).toDF("id", "name", "age")

      // Create an Iceberg table in S3/MinIO
      spark.sql(f"CREATE NAMESPACE IF NOT EXISTS mycatalog.demo")
      spark.sql(f"USE mycatalog")
      spark.sql(
        f"""
        CREATE TABLE IF NOT EXISTS mycatalog.demo.users (
          id INT,
          name STRING,
          age INT
        ) USING iceberg
        LOCATION 's3a://iceberg-demo/warehouse/demo/'
        """
      )

      df.writeTo("demo.users").append()

      // Read data from the Iceberg table
      val result = spark.table("demo.users")
      println("Data from Iceberg table in S3/MinIO:")
      result.show()

      // Example of reading specific partitions
      spark.table("demo.users").where("age > 30").show()

    } finally {
      spark.stop()
    }
  }
} 