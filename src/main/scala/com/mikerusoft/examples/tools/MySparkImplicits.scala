package com.mikerusoft.examples.tools

import com.mikerusoft.examples.config.IcebergConf
import org.apache.iceberg.io.LocationProvider
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Dataset, Row, SparkSession}

object MySparkImplicits {

  implicit class DataFrameWrapper(df: Dataset[Row]) {
    def createCatalogIfDoesNotExist(iceberg: IcebergConf): Dataset[Row] = {
      df.sparkSession.catalogIfDoesNotExist(iceberg)
      df
    }
    def createTableOfDoesNotExist(iceberg: IcebergConf, mayBePartitionBy: List[PartitionBy]): Dataset[Row] = {
      df.sparkSession.tableOfDoesNotExist(iceberg, df.schema, mayBePartitionBy)
      df
    }
  }

  implicit class SparkSessionWrapper(spark: SparkSession) {
    def catalogIfDoesNotExist(iceberg: IcebergConf): SparkSession = {
      val doesDbExist = spark.catalog.databaseExists(iceberg.fullDbName)
      if (!doesDbExist) {
        val createDbSql = s"CREATE NAMESPACE IF NOT EXISTS ${iceberg.fullDbName}"
        // Create table with Parquet schema
        println(s"Creating new db:\n$createDbSql")
        spark.sql(createDbSql)
      }
      spark.sql(s"USE ${iceberg.catalogName}")
      spark
    }

    def tableOfDoesNotExist(iceberg: IcebergConf, parquetSchema: StructType, mayBePartitionBy: List[PartitionBy]): SparkSession = {
      val tableExists = spark.catalog.tableExists(iceberg.fullDbName, iceberg.table)
      if (!tableExists) {
        val partitionBy = mayBePartitionBy match {
          case List() => ("", "")
          case l: List[PartitionBy] =>
            (l.map(_.expr()).mkString("PARTITIONED BY (", ", ", ")"), s",'write.location-provider.flat.fields'='${l.head.column}'")
        }

        val createTableSql = s"""
              CREATE TABLE IF NOT EXISTS ${iceberg.fullTableName} (
                ${parquetSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n\t\t")},
                ts Timestamp
              ) USING iceberg
              ${partitionBy._1}
              TBLPROPERTIES (
                  'write.location-provider.impl'='com.mikerusoft.examples.FlatLocationProvider'
                  ${partitionBy._2}
              )
            """
        // partition: days(ts) automatically contains years, months, days
        // Create table with Parquet schema
        println(s"Creating new table with schema:\n$createTableSql")
        spark.sql(createTableSql)
      }
      spark
    }
  }
}
