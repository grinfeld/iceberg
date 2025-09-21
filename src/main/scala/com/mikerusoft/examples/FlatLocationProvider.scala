package com.mikerusoft.examples

import org.apache.iceberg.{LocationProviders, PartitionData, PartitionField, PartitionSpec, Schema, StructLike}
import org.apache.iceberg.io.LocationProvider
import org.apache.iceberg.types.Types
import org.apache.iceberg.types.Types.StructType

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

class FlatLocationProvider(tableLocation: String, conf: java.util.Map[String, String]) extends LocationProvider {
  private val (updatedConf, isNewConf) = {
    // I need to find original location provider here
    val location = conf.get("write.location-provider.impl")
    if (this.getClass.getCanonicalName.equals(location)) {
      val newMap = new java.util.HashMap[String, String](conf)
      newMap.remove("write.location-provider.impl")
      (newMap, true)
    } else {
      (conf, false)
    }
  }
  val location: LocationProvider = LocationProviders.locationsFor(tableLocation, updatedConf)

  override def newDataLocation(s: String): String = {
    location.newDataLocation(s)
  }

  override def newDataLocation(partitionSpec: PartitionSpec, structLike: StructLike, s: String): String = {
    val listOfFields = 
      updatedConf.getOrDefault("write.location-provider.flat.fields", "").split(",").map(_.trim).filter(_.nonEmpty)
    if (!isNewConf || PartitionSpec.unpartitioned().equals(partitionSpec) || partitionSpec.fields().size() <= 0) {
      location.newDataLocation(partitionSpec, structLike, s)
    } else if (listOfFields.isEmpty) {
      location.newDataLocation(PartitionSpec.unpartitioned(), structLike, s)
    } else {
      // Filter partition fields based on listOfFields
      val fieldsToInclude = partitionSpec.fields().asScala.filter { field =>
        val sourceField = partitionSpec.schema().findField(field.sourceId())
        listOfFields.contains(sourceField.name())
      }.toList
      
      // If no fields match, fallback to default behavior
      val originalType = partitionSpec.partitionType()
      // Collect source fields for the filtered partition fields
      val sourceFields: Seq[Types.NestedField] = fieldsToInclude.map { field =>
        partitionSpec.schema().findField(field.sourceId())
      }

      // Create schema with only the filtered source fields
      val newSchema = new Schema(sourceFields.asJava)

      // Create new PartitionSpec with filtered fields
      var builder: PartitionSpec.Builder = PartitionSpec.builderFor(newSchema)
      fieldsToInclude.foreach { field =>
        val sourceField = partitionSpec.schema().findField(field.sourceId())
        builder = field.transform().toString match {
          case "identity" => builder.identity(sourceField.name())
          case s if s.startsWith("bucket") =>
            val numBuckets = field.transform().toString.replaceAll("[^0-9]", "").toInt
            builder.bucket(sourceField.name(), numBuckets)
          case s if s.startsWith("truncate") =>
            val width = field.transform().toString.replaceAll("[^0-9]", "").toInt
            builder.truncate(sourceField.name(), width)
          case "year" => builder.year(sourceField.name())
          case "month" => builder.month(sourceField.name())
          case "day" => builder.day(sourceField.name())
          case "hour" => builder.hour(sourceField.name())
          case _ => throw new UnsupportedOperationException(s"Unsupported transform: ${field.transform()}")
        }
      }

      val newSpec = builder.build()

      // Create partial StructLike (PartitionData) with filtered fields
      val filteredFields = fieldsToInclude.zipWithIndex.map { case (field, idx) =>
        val originalIndex = partitionSpec.fields().asScala.indexOf(field)
        originalType.fields().get(originalIndex)
      }
      val newStructType = StructType.of(filteredFields.asJava)
      val partialData = new PartitionData(newStructType)

      // Copy the values from original structLike for filtered fields
      fieldsToInclude.zipWithIndex.foreach { case (field, newIdx) =>
        val originalIndex = partitionSpec.fields().asScala.indexOf(field)
        val fieldSchema = partitionSpec.partitionType.asSchema().columns().get(originalIndex)
        val value = structLike.get(originalIndex, fieldSchema.`type`().typeId().javaClass())
        partialData.set(newIdx, value)
      }

      // Now use the partial spec and data
      val path = location.newDataLocation(newSpec, partialData, s)
      path
    }
  }
}
