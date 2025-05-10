package com.example

case class IcebergConf(catalogType: String, database: String, table: String) {
  def fullTableName: String = {
    s"$database.$table"
  }

  def fullDbName: String = {
    s"$database"
  }
}
