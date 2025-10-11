package com.mikerusoft.examples.config

import com.mikerusoft.examples.tools.{PartitionBy, Bucket, Field, DateType, Truncate}
import com.typesafe.config.{Config, ConfigFactory, ConfigValue, ConfigValueFactory, ConfigValueType}
import org.apache.spark.sql.SparkSession

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ConfigParser {
  implicit class ConfigParser (config: Config) {
    lazy val iceberg: IcebergConf = createIcebergConf()
    lazy val partitionByFields: List[PartitionBy] = getPartitionByFields()

    private def createIcebergConf(): IcebergConf = {
      val catalogName = config.getConfig("app.config.spark.sql.catalog").entrySet()
        .asScala
        .filter(e => e.getKey.matches("^.+\\.type"))
        .map(e => e.getKey.substring(0, e.getKey.length - 5))
        .take(1).toList match {
        case List() => ""
        case ::(head, next) => head
      }
      IcebergConf(catalogName, config.getString("app.iceberg.database"), config.getString("app.iceberg.table"))
    }

    private def getPartitionByFields(): List[PartitionBy] = {
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

    def getPartitionBy: List[PartitionBy] = {
      partitionByFields
    }

    def sparkSession(): SparkSession = {
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

      sparkBuilder.getOrCreate()
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
