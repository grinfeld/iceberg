package com.mikerusoft.examples.tableproperties

import com.mikerusoft.examples.tools.PartitionBy

sealed trait TableProperty {
  def key: String
  def value: String

  def writeValue(): String = s"\t\t'$key'='$value'"
}

case class StringProperty(key: String, value: String) extends TableProperty

case class FlatTableProperty(partitionBy: List[PartitionBy]) extends TableProperty {

  override def key: String = "write.location-provider.impl"
  override def value: String = "com.mikerusoft.examples.FlatLocationProvider"

  override def writeValue(): String = {
    val locationProviderProperty = super.writeValue()
    if (partitionBy.isEmpty) {
      locationProviderProperty
    } else {
      val columnsToFlatBy = partitionBy.map(_.column).mkString(",")
      locationProviderProperty + s",\n\t\t'write.location-provider.flat.fields'='$columnsToFlatBy'"
    }
  }
}

object FlatTableProperty {
  def apply(partitionBy: PartitionBy*): FlatTableProperty = new FlatTableProperty(partitionBy.toList)
}