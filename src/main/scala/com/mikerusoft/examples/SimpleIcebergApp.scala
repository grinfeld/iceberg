package com.mikerusoft.examples

import com.mikerusoft.examples.compaction.{Binpack, MinFileSizeBytes, MinInputFiles, TargetFileSizeBytes}
import com.mikerusoft.examples.config.ConfigParser.{ConfigParser, ConfigStarter}
import com.mikerusoft.examples.config.IcebergConf
import com.mikerusoft.examples.tools.MySparkImplicits.{AfterWriteAction, DataFrameWrapper, DataFrameWriterV2Write}
import org.apache.spark.sql.SparkSession
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
        .writeTo(icebergConf.fullTableName)
        .appendAnd()
      .compact(config.icebergConf(), Binpack(MinInputFiles(1)))

    println("\nData in Iceberg table:")
    spark.table(icebergConf.fullTableName).show(5, truncate = false)

    spark.stop()
  }
}
