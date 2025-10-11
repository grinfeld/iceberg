name := "spark-iceberg-example"
version := "0.1.0"
scalaVersion := "2.13.16"

// Java 11 configuration
javacOptions ++= Seq("-source", "17", "-target", "17")
scalacOptions += "-target:jvm-17"

// IDE specific settings for better code completion
Global / semanticdbEnabled := true
Global / semanticdbVersion := "4.9.6"
ThisBuild / autoCompilerPlugins := true

// versions are matter. This is a matrix that works around the spark 3.5.5
val Versions = new {
  val spark = "4.0.0"
  val hadoop = "3.4.2"
  val aws = "2.33.11"
  val iceberg = "1.10.0"
  val log4j = "2.20.0"
  val jackson = "2.15.3"
  val slf4j = "2.0.7"
  val scalaLogger = "3.9.6"
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

  ("org.apache.iceberg" % "iceberg-spark-runtime-4.0_2.13" % Versions.iceberg)
    .exclude("org.apache.parquet", "parquet-common")
    .exclude("org.apache.parquet", "parquet-hadoop")
    .exclude("org.apache.parquet", "parquet-column"),
  ("org.apache.iceberg" % "iceberg-aws-bundle" % Versions.iceberg)
    .exclude("com.fasterxml.jackson.core", "jackson-databind")
    .exclude("com.fasterxml.jackson.core", "jackson-core")
    .exclude("com.fasterxml.jackson.core", "jackson-annotations")
    .exclude("org.apache.parquet", "parquet-common")
    .exclude("org.apache.parquet", "parquet-hadoop")
    .exclude("org.apache.parquet", "parquet-column")
  ,

  ("org.apache.hive" % "hive-metastore" % Versions.hive)
    .exclude("org.apache.parquet", "parquet-hadoop") exclude("org.apache.thrift", "libthrift"),

  "com.typesafe" % "config" % "1.4.3",
  "org.slf4j" % "slf4j-api" % Versions.slf4j,
  "org.apache.logging.log4j" % "log4j-api" % Versions.log4j,
  "org.apache.logging.log4j" % "log4j-core" % Versions.log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % Versions.log4j,
  "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogger,
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
