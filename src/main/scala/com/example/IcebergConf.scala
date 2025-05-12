package com.example

case class IcebergConf(catalogType: String, database: String, table: String) {
  def fullTableName: String = {
    s"hive.$database.$table"
  }

  def fullDbName: String = {
    s"hive.$database"
  }
}
