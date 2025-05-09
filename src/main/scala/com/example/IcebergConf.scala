package com.example

case class IcebergConf(database: String, table: String) {
  def fullTableName: String = {
    s"$database.$table"
  }
}
