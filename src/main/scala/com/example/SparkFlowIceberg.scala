package com.example

import com.typesafe.config.{Config, ConfigFactory, ConfigValue, ConfigValueType}
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
        // Check if table exists
        val tableExists = spark.catalog.tableExists(iceberg.database, iceberg.table)
        if (!tableExists) {
          // Read Parquet data to get schema
          val parquetPath = config.getString("app.s3.from")
          val parquetSchema = spark.read.parquet(parquetPath).schema
          
          // Create table with Parquet schema
          println("Creating new table with schema:")
          spark.sql(
            s"""
              CREATE TABLE ${iceberg.fullTableName} (
                ${parquetSchema.fields.map(field => s"${field.name} ${field.dataType.sql}").mkString(",\n\t\t")}
              ) USING iceberg
            """)
        }
      }

  }

  implicit class ConfigParser (config: Config) {

    def sparkFlow(): SparkFlowIceberg = {
      val appConfig: Config = config.getConfig("app")
      var sparkBuilder = SparkSession.builder()
        .appName(appConfig.getString("name"))
      if(appConfig.hasPath("master"))
        sparkBuilder = sparkBuilder.master(appConfig.getString("master"))

      applyConfig(config.getConfig("app.config"), "", (key, value) => {
        value match {
          case d: Double => sparkBuilder.config(key, d)
          case l: Long => sparkBuilder.config(key, l)
          case b: Boolean => sparkBuilder.config(key, b)
          case any: Any => sparkBuilder.config(key, any.toString)
        }
      })

      SparkFlowIceberg(sparkBuilder.getOrCreate(), config)
    }

    def icebergConf(): IcebergConf = {
      IcebergConf(config.getString("app.iceberg.database"), config.getString("app.iceberg.table"))
    }

    private def applyConfig(config: Config, prefix: String = "", setFn: (String, Any) => Unit): Unit = {
      config.entrySet().forEach { entry =>
        val key = entry.getKey
        val value: ConfigValue = entry.getValue

        val nextPrefix = if ("".equals(prefix)) key else s"$prefix.$key"
        if ("def-impl".equals(key)) {
          // spark.conf.set("spark.sql.catalog.<catalog name>", "org.apache.iceberg.spark.SparkCatalog")
          // or custom one
          setFn(nextPrefix, value.unwrapped().toString)
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
    }
  }
}
