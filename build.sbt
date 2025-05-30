name := "spark-iceberg-example"
version := "0.1.0"
scalaVersion := "2.12.18"

// Java 11 configuration
javacOptions ++= Seq("-source", "11", "-target", "11")
scalacOptions += "-target:jvm-11"

// IDE specific settings for better code completion
Global / semanticdbEnabled := true
Global / semanticdbVersion := "4.8.8"
ThisBuild / autoCompilerPlugins := true

// versions are matter. This is a matrix that works around the spark 3.5.5
val Versions = new {
  val spark = "3.5.5"
  val hadoop = "3.3.6"
  val aws = "2.20.143"
  val iceberg = "1.5.0"
  val log4j = "2.20.0"
  val jackson = "2.15.3"
  val slf4j = "2.0.7"
  val hive = "3.1.3"
  val parquet = "1.13.1"
  val parquetFormat = "2.9.0"
  val thrift = "0.13.0"
}

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Versions.hadoop,
  ("org.apache.hadoop" % "hadoop-aws" % Versions.hadoop)
    .exclude("com.fasterxml.jackson.core", "jackson-databind")
    .exclude("com.fasterxml.jackson.core", "jackson-core")
    .exclude("com.fasterxml.jackson.core", "jackson-annotations"),

  "software.amazon.awssdk" % "s3" % Versions.aws,
  "software.amazon.awssdk" % "sts" % Versions.aws,
  "software.amazon.awssdk" % "glue" % Versions.aws,

  ("org.apache.iceberg" % "iceberg-spark-runtime-3.5_2.12" % Versions.iceberg)
    .exclude("org.apache.parquet", "parquet-common")
    .exclude("org.apache.parquet", "parquet-hadoop")
    .exclude("org.apache.parquet", "parquet-column"),
  ("org.apache.iceberg" % "iceberg-aws" % Versions.iceberg)
    .exclude("com.fasterxml.jackson.core", "jackson-databind")
    .exclude("com.fasterxml.jackson.core", "jackson-core")
    .exclude("com.fasterxml.jackson.core", "jackson-annotations")
    .exclude("org.apache.parquet", "parquet-common")
    .exclude("org.apache.parquet", "parquet-hadoop")
    .exclude("org.apache.parquet", "parquet-column")
  ,

  ("org.apache.iceberg" % "iceberg-hive-runtime" % "1.7.2")
    .exclude("org.apache.parquet", "parquet-common")
    .exclude("org.apache.parquet", "parquet-hadoop")
    .exclude("org.apache.parquet", "parquet-column")
    .exclude("org.apache.thrift", "libthrift")
  ,
  ("org.apache.hive" % "hive-metastore" % Versions.hive)
    .exclude("org.apache.parquet", "parquet-hadoop") exclude("org.apache.thrift", "libthrift"),

  "com.typesafe" % "config" % "1.4.3",
  "org.slf4j" % "slf4j-api" % Versions.slf4j,
  "org.apache.logging.log4j" % "log4j-api" % Versions.log4j,
  "org.apache.logging.log4j" % "log4j-core" % Versions.log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % Versions.log4j,
  "org.apache.thrift" % "libthrift" % Versions.thrift,

  // Test dependencies
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.testcontainers" % "testcontainers" % "1.21.0" % Test,
  "io.minio" % "minio" % "8.5.17" % Test,
  "com.squareup.okhttp3" % "okhttp" % "4.12.0" % Test
)

libraryDependencies ++= Seq("org.apache.spark" %% "spark-sql" % Versions.spark)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % Versions.jackson,
  "com.fasterxml.jackson.core" % "jackson-core" % Versions.jackson,
  "com.fasterxml.jackson.core" % "jackson-annotations" % Versions.jackson,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % Versions.jackson,
  ("org.apache.hive" % "hive-metastore" % Versions.hive) exclude("org.apache.parquet", "parquet-hadoop") exclude("org.apache.thrift", "libthrift"),
  ("org.apache.hive" % "hive-exec" % Versions.hive) exclude("org.apache.parquet", "parquet-hadoop") exclude("org.apache.thrift", "libthrift"),
  ("org.apache.hive" % "hive-common" % Versions.hive) exclude("org.apache.parquet", "parquet-hadoop") exclude("org.apache.thrift", "libthrift"),
  "org.apache.parquet" % "parquet-format" % Versions.parquetFormat,
  "org.apache.parquet" % "parquet-common" % Versions.parquet,
  "org.apache.parquet" % "parquet-column" % Versions.parquet,
  "org.apache.parquet" % "parquet-encoding" % Versions.parquet,
  "org.apache.parquet" % "parquet-hadoop" % Versions.parquet,
  "org.apache.parquet" % "parquet-hadoop-bundle" % Versions.parquet
)
