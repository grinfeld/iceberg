package com.mikerusoft.examples

import com.mikerusoft.examples.compaction.{Binpack, MinInputFiles}
import com.mikerusoft.examples.config.ConfigParser.{ConfigParser, ConfigStarter}
import com.mikerusoft.examples.config.IcebergConf
import com.mikerusoft.examples.tools.MySparkImplicits.{DataFrameWrapper, DataFrameWriterV2Write}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, timestamp_millis}
import org.slf4j.{Logger, LoggerFactory}

object SimpleIcebergApp {
  val log: Logger = LoggerFactory.getLogger(SimpleIcebergApp.getClass.getName)
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
      // will do nothing if catalog type is 'glue'
      .compact(config.icebergConf(), Binpack(MinInputFiles(1)))

    log.info("\nData in Iceberg table:")
    spark.table(icebergConf.fullTableName).show(5, truncate = false)

    spark.stop()
  }
}
