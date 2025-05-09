package com.example

import com.typesafe.config.{Config, ConfigBeanFactory, ConfigFactory, ConfigValueType}
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

case class ConfigParser private(config: Config) {

  // Apply all Spark configurations from the config file
  def applyConfig(key: String, sparkBuilder: SparkSession.Builder): SparkSession.Builder  = {
    applyConfig(config.getConfig(key), "", sparkBuilder)
    sparkBuilder
  }

  def icebergConf(): IcebergConf = {
    IcebergConf(config.getString("app.iceberg.database"), config.getString("app.iceberg.table"))
  }

  private def applyConfig(config: Config, prefix: String = "", sparkBuilder: SparkSession.Builder): Unit = {
    config.entrySet().forEach { entry =>
      val key = entry.getKey
      val value = entry.getValue

      val nextPrefix = if ("".equals(prefix)) key else s"$prefix.$key"
      if ("def-impl".equals(key)) {
        // spark.conf.set("spark.sql.catalog.<catalog name>", "org.apache.iceberg.spark.SparkCatalog")
        // or custom one
        sparkBuilder.config(key, value.unwrapped().toString)
      } else {
        value.valueType() match {
          case ConfigValueType.OBJECT =>
            applyConfig(config.getConfig(key), nextPrefix, sparkBuilder)
          case ConfigValueType.LIST =>
            if ("".equals(nextPrefix))
              throw new IllegalArgumentException("in spark list can not start with empty config")
            val list = config.getList(key).unwrapped().asScala.zipWithIndex
            // we assume all of them string
            for ((value, ind) <- list) {
              sparkBuilder.config(s"$nextPrefix[$ind]", value.toString)
            }
            val res = config.getList(key).unwrapped().asScala.map(_.toString).mkString(",")
            sparkBuilder.config(nextPrefix, res)
          case ConfigValueType.NULL => return
          case ConfigValueType.STRING =>
            sparkBuilder.config(nextPrefix, config.getString(key))
          case ConfigValueType.STRING =>
            sparkBuilder.config(nextPrefix, config.getBoolean(key))
          case ConfigValueType.NUMBER =>
            if (value.unwrapped().toString.contains("."))
              sparkBuilder.config(nextPrefix, value.unwrapped().asInstanceOf[Number].doubleValue())
            else
              sparkBuilder.config(nextPrefix, value.unwrapped().asInstanceOf[Number].longValue())
        }
      }
    }
  }
}

object ConfigParser {
  def apply(profile: String): ConfigParser = {
    val config = ConfigFactory.load(s"application.$profile.conf")
    new ConfigParser(config)
  }
}
