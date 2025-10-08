package com.mikerusoft.examples.tools

trait PartitionBy {
  def expr(): String
}

case class Field(column: String) extends PartitionBy {
  def expr(): String = column
}
case class Bucket(column: String, size: Int) extends PartitionBy {
  def expr() = s"bucket($size, $column)"
}
case class Truncate(column: String, size: Int) extends PartitionBy {
  def expr() = s"truncate($size, $column)"
}
case class DateType private(column: String, func: String) extends PartitionBy {
  def expr() = s"$func($column)"
}

object DateType {
  def apply(column: String, funcName: String):DateType = {
    funcName match {
      case "year" => new DateType(column, funcName)
      case "month" => new DateType(column, funcName)
      case "day" => new DateType(column, funcName)
      case "hour" => new DateType(column, funcName)
      case _ => throw new IllegalArgumentException("Only year, day, month and hour supported")
    }
  }
}