package com.example

import com.typesafe.config.{Config, ConfigFactory, ConfigValue, ConfigValueFactory, ConfigValueType}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object SparkFlowIceberg {

  case class SparkFlowIceberg(spark: SparkSession, config: Config) {

      def readParquet(): DataFrame = {
        val df = spark.read.parquet(config.getString("app.s3.from"))
        createTableIfNotExist()
        df
      }

      def stop(): Unit = {
        spark.stop()
      }

      def table(name: String): DataFrame = {
        spark.table(name)
      }

      def icebergConf(): IcebergConf = {
        config.icebergConf()
      }

      private def createTableIfNotExist(): Unit = {
        val iceberg: IcebergConf = config.icebergConf()

/*        spark.sql(s"DROP table ${iceberg.fullTableName}")
        spark.sql(s"DROP NAMESPACE ${iceberg.fullDbName}")*/

        val doesDbExist = spark.catalog.databaseExists(iceberg.fullDbName)
        if (!doesDbExist) {
          val createDbSql = s"CREATE NAMESPACE IF NOT EXISTS ${iceberg.fullDbName}"
          // Create table with Parquet schema
          println(s"Creating new db:\n$createDbSql")
          spark.sql(createDbSql)
        }
        // Check if table exists
        val tableExists = spark.catalog.tableExists(iceberg.fullDbName, iceberg.table)
        if (!tableExists) {
          // Read Parquet data to get schema
          val parquetPath = config.getString("app.s3.from")
          spark.read.parquet(parquetPath).show(1)
          val parquetSchema = spark.read.parquet(parquetPath).schema

          val partitionBy = config.getPartitionBy() match {
            case List() => ""
            case l: List[PartitionBy] => l.map(_.expr()).mkString("PARTITIONED BY (", ", ", ")")
          }

          val createTableSql = s"""
              CREATE TABLE IF NOT EXISTS ${iceberg.fullTableName} (
                ${parquetSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n\t\t")},
                ts Timestamp
              ) USING iceberg
              $partitionBy
            """
          // partition: days(ts) automatically contains years, months, days
          // Create table with Parquet schema
          println(s"Creating new table with schema:\n$createTableSql")
          spark.sql(createTableSql)
        }
      }
  }

  implicit class ConfigParser (config: Config) {
    lazy val iceberg: IcebergConf = createIcebergConf()

    private def createIcebergConf(): IcebergConf = {
      val catalogType = config.getConfig("app.config.spark.sql.catalog").entrySet()
        .asScala.filter(e => e.getKey.matches("^.+\\.type"))
        .map(_.getValue.unwrapped().asInstanceOf[String])
        .take(1).fold("")((a1, a2) => a1 + a2)
      IcebergConf(catalogType, config.getString("app.iceberg.database"), config.getString("app.iceberg.table"))
    }

    def getPartitionBy(): List[PartitionBy] = {
      if (!config.hasPath("app.partition-by"))
        return List()
      config.getConfigList("app.partition-by")
        .asScala
        .map(conf => {
          val col = conf.getString("col")
          conf.getString("type") match  {
            case "identity" => Field(col)
            case "bucket" => Bucket(col, conf.getInt("size"))
            case "date" => DateType(col, conf.getString("func"))
            case "truncate" => Truncate(col, conf.getInt("size"))
            case v: Any => throw new IllegalArgumentException("Only identity, bucket or date supported. But received " + v.toString)
          }
        }).toList
    }

    def sparkFlow(): SparkFlowIceberg = {
      val appConfig: Config = config.getConfig("app")
      var sparkBuilder = SparkSession.builder()
        .appName(appConfig.getString("name"))
      if(appConfig.hasPath("master"))
        sparkBuilder = sparkBuilder.master(appConfig.getString("master"))

      applyConfig(config.getConfig("app.config"), "", (key, value) => {
        value match {
          case d: Double => sparkBuilder = sparkBuilder.config(key, d)
          case l: Long => sparkBuilder = sparkBuilder.config(key, l)
          case b: Boolean => sparkBuilder = sparkBuilder.config(key, b)
          case any: Any => sparkBuilder = sparkBuilder.config(key, any.toString)
        }
      })

      val spark = sparkBuilder.getOrCreate()
      SparkFlowIceberg(spark, config)
    }

    def icebergConf(): IcebergConf = {
      iceberg
    }

    private def applyConfig(config: Config, prefix: String = "", setFn: (String, Any) => Unit): Unit = {
      config.entrySet().forEach { entry =>
        val key = entry.getKey
        val value: ConfigValue = entry.getValue

        val nextPrefix = if ("".equals(prefix)) key else s"$prefix.$key"
        if (key.endsWith(".def-impl")) {
          // spark.conf.set("spark.sql.catalog.<catalog name>", "org.apache.iceberg.spark.SparkCatalog")
          // or custom one
          setFn(key.replace(".def-impl", ""), value.unwrapped().toString)
        } else {
          value.valueType() match {
            case ConfigValueType.LIST =>
              if ("".equals(nextPrefix))
                throw new IllegalArgumentException("in spark: list can not start with empty config")
              val list = config.getList(key).unwrapped().asScala.zipWithIndex
              // we assume all of them string
              for ((value, ind) <- list) {
                setFn(s"$nextPrefix[$ind]", value.toString)
              }
              val res = config.getList(key).unwrapped().asScala.map(_.toString).mkString(",")
              setFn(nextPrefix, res)
            case ConfigValueType.NULL => return
            case ConfigValueType.STRING =>
              setFn(nextPrefix, config.getString(key))
            case ConfigValueType.BOOLEAN =>
              setFn(nextPrefix, config.getBoolean(key))
            case ConfigValueType.NUMBER =>
              if (value.unwrapped().toString.contains("."))
                setFn(nextPrefix, value.unwrapped().asInstanceOf[Number].doubleValue())
              else
                setFn(nextPrefix, value.unwrapped().asInstanceOf[Number].longValue())
            case ConfigValueType.OBJECT =>
              applyConfig(config.getConfig(nextPrefix), nextPrefix, setFn)
          }
        }
      }
    }
  }

  implicit class ConfigStarter(profile: String) {
    def createConfig(): Config = {
      ConfigFactory.load(s"application.$profile.conf")
        .withFallback(ConfigFactory.load("application.conf"))
        .withValue("profile", ConfigValueFactory.fromAnyRef(profile))
    }
  }
}
