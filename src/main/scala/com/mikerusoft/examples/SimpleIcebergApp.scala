package com.mikerusoft.examples

import com.mikerusoft.examples.MySparkImplicits.DataFrameWrapper
import com.mikerusoft.examples.config.ConfigParser.{ConfigParser, ConfigStarter}
import com.mikerusoft.examples.config.IcebergConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, timestamp_millis}

object SimpleIcebergApp {
  def runApp(profile: String): Unit = {
    val config = profile.createConfig()
    val spark: SparkSession = config.sparkSession()
    val icebergConf: IcebergConf = config.icebergConf()

    spark.read.parquet(config.getString("app.s3.from"))
        .createCatalogIfDoesNotExist(icebergConf)
        .createTableOfDoesNotExist(icebergConf, config.getPartitionBy)
        .withColumn("ts", timestamp_millis(col("resolvedTimestamp")))
        .writeTo(icebergConf.fullTableName).append()

    // Print current schema and data
    println("\nCurrent table schema:")
    spark.table(icebergConf.fullTableName).printSchema()

    println("\nData in Iceberg table:")
    spark.table(icebergConf.fullTableName).show(5, truncate = false)

    spark.stop()
  }
}
