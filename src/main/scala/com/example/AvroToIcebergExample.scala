package com.example

import com.example.SparkFlowIceberg.{ConfigParser, ConfigStarter}

object AvroToIcebergExample {
  def main(args: Array[String]): Unit = {
    val profile = System.getProperty("PROFILE")

    val sparkFlow =  profile
      .createConfig()
      .sparkFlow()

    val icebergConf = sparkFlow.icebergConf()

    // Write to Iceberg table - Iceberg will automatically handle schema evolution
    val rawEvents = sparkFlow.readParquet()

    // Write to Iceberg table - Iceberg will automatically handle schema evolution
    rawEvents.writeTo(icebergConf.fullTableName).append()

    // Print current schema and data
    println("\nCurrent table schema:")
    sparkFlow.spark.table(icebergConf.fullTableName).printSchema()
    
    println("\nData in Iceberg table:")
    sparkFlow.spark.table(icebergConf.fullTableName).show(5, truncate = false)

    sparkFlow.spark.stop()
  }


} 