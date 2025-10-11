package com.mikerusoft.examples.tools

import com.mikerusoft.examples.config.IcebergConf
import com.mikerusoft.examples.compaction.Strategy
import com.mikerusoft.examples.tableproperties.TableProperty
import org.apache.spark.sql.catalyst.analysis.{CannotReplaceMissingTableException, NoSuchTableException, TableAlreadyExistsException}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrameWriterV2, Dataset, Row, SparkSession}

object MySparkImplicits {

  class SessionAlreadyClosed (message: String) extends Exception(message: String) {}

  class AfterWriteAction(sparkOpt: Option[SparkSession]) {

    @throws(classOf[SessionAlreadyClosed])
    def compact(icebergConf: IcebergConf, strategy: Strategy): Unit = {
      sparkOpt match {
        case None => throw new SessionAlreadyClosed("Session is already closed")
        case Some(spark) =>
          val sql =  f"""
            |CALL ${icebergConf.catalogName}.system.rewrite_data_files(
            |  table => '${icebergConf.tableDB}',
            |  ${strategy.write()}
            |)
          |""".stripMargin
          spark.table(icebergConf.fullTableName).show(1, truncate = false)
          val res = spark.sql(sql).collectAsList()
          println(res)
      }
    }
  }

  implicit class DataFrameWriterV2Write[T](writer: DataFrameWriterV2[T]) {

    private def withAfterWriteAction(fn: () => Unit): AfterWriteAction = {
      val spark: Option[SparkSession] = SparkSession.getActiveSession
      fn()
      new AfterWriteAction(spark)
    }

    @throws(classOf[NoSuchTableException])
    def appendAnd(): AfterWriteAction = {
      withAfterWriteAction(writer.append)
    }

    @throws(classOf[TableAlreadyExistsException])
    def createAnd(): AfterWriteAction = {
      withAfterWriteAction(writer.create)
    }

    @throws(classOf[CannotReplaceMissingTableException])
    def replaceAnd(): AfterWriteAction= {
      withAfterWriteAction(writer.replace)
    }

    def createOrReplaceAnd(): AfterWriteAction= {
      withAfterWriteAction(writer.createOrReplace)
    }

  }

  implicit class DataFrameWrapper(df: Dataset[Row]) {
    def createCatalogIfDoesNotExist(iceberg: IcebergConf): Dataset[Row] = {
      df.sparkSession.catalogIfDoesNotExist(iceberg)
      df
    }
    def createTableOfDoesNotExist(iceberg: IcebergConf, mayBePartitionBy: List[PartitionBy], tableProperties: TableProperty*): Dataset[Row] = {
      df.sparkSession.tableOfDoesNotExist(iceberg, df.schema, mayBePartitionBy, tableProperties.toList)
      df
    }
  }

  implicit class SparkSessionWrapper(spark: SparkSession) {

    def catalogIfDoesNotExist(iceberg: IcebergConf): SparkSession = {
      val doesDbExist = spark.catalog.databaseExists(iceberg.fullDbName)
      if (!doesDbExist) {
        val createDbSql = s"CREATE NAMESPACE IF NOT EXISTS ${iceberg.fullDbName}"
        // Create table with Parquet schema
        println(s"Creating new db:\n${iceberg.fullDbName}")
        println(spark.sql(createDbSql).collectAsList())
      }
      spark.sql(s"USE ${iceberg.catalogName}")
      spark
    }

    def tableOfDoesNotExist(iceberg: IcebergConf, parquetSchema: StructType, mayBePartitionBy: List[PartitionBy] = List(), tableProperties: List[TableProperty] = List()): SparkSession = {
      val tableExists = spark.catalog.tableExists(iceberg.fullDbName, iceberg.table)
      if (!tableExists) {
        val partitionBy = mayBePartitionBy match {
          case List() => ""
          case l: List[PartitionBy] =>
            l.map(_.expr()).mkString("PARTITIONED BY (", ", ", ")")
        }

        val createTableSql = s"""
          | CREATE TABLE IF NOT EXISTS ${iceberg.fullTableName} (
          |   ${parquetSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n\t\t")},
          |   ts Timestamp
          | ) USING iceberg
          | $partitionBy
        |""".stripMargin
        val tableProps = if (tableProperties.isEmpty) "" else tableProperties.map(_.writeValue()).mkString("TBLPROPERTIES (\n", ",\n\t\t", "\n)")
        // partition: days(ts) automatically contains years, months, days
        // Create table with Parquet schema
        // println(s"Creating new table with schema:\n$createTableSql")
        val tableStructure = (if (tableProps.isEmpty) createTableSql else (createTableSql + "\n" + tableProps))
        println(spark.sql(tableStructure).collectAsList())
      }
      spark
    }
  }
}
