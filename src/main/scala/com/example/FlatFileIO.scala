/*
package com.example

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.iceberg.aws.s3.S3FileIO
import org.apache.iceberg.hadoop.{HadoopFileIO, HadoopOutputFile, SerializableConfiguration}
import org.apache.iceberg.io.{FileIO, InputFile, OutputFile}

import java.io.IOException
import java.net.URI
import java.util

class FlatFileIO extends FileIO {

  private val conf: SerializableConfiguration = null
  private var inner: FileIO = _

  override def initialize(properties: util.Map[String, String]): Unit = {
    val tpe = properties.getOrDefault("type", "")
    // todo - actually we should look another way to find what type of output should be
    tpe match {
      case "hive" =>
        inner = new S3FileIO()
        inner.initialize(properties)
      case "hadoop" =>
        inner = new HadoopFileIO()
        inner.initialize(properties)
    }
  }

  override def properties: util.Map[String, String] = {
    inner.properties()
  }

  override def newOutputFile(path: String): OutputFile = {
    if (path.endsWith("metadata.json")) return super.newOutputFile(path)
    // Flatten the directory layout by ignoring partition-based paths
    // For example, always put files into s3://bucket/flat/
    val flatPath = flattenPath(path)
    HadoopOutputFile.fromLocation(flatPath, conf.get)
  }

  private def flattenPath(path: String): String = {
    // Example: rewrite everything to flat structure in S3
    val uri = URI.create(path)
    val fileName = path.substring(path.lastIndexOf('/') + 1)
    val flatPath = "s3://my-bucket/flat/" + fileName
    flatPath
  }

  override def deleteFile(path: String): Unit = {
    try {
      val filePath = new Path(path)
      val fs = filePath.getFileSystem(conf.get)
      fs.delete(filePath, false)
    } catch {
      case e: IOException =>
        throw new RuntimeException(e)
    }
  }

  override def close(): Unit = {

    // No resources to clean up
  }

  override def newInputFile(s: String): InputFile = inner.newInputFile(s)
}
*/
