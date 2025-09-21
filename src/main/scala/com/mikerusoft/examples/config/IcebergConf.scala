package com.mikerusoft.examples.config

case class IcebergConf(catalogName: String, database: String, table: String) {
  def fullTableName: String = {
    s"$catalogName.$database.$table"
  }

  def fullDbName: String = {
    s"$catalogName.$database"
  }
}
