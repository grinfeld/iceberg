package com.example

case class IcebergConf(catalogType: String, database: String, table: String) {
  def fullTableName: String = {
    s"$catalogType.$database.$table"
  }

  def fullDbName: String = {
    s"$catalogType.$database"
  }
}
