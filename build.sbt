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
  val iceberg = "1.9.0"
  val log4j = "2.20.0"
  val jackson = "2.15.3"
  val slf4j = "2.0.7"
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

  "org.apache.iceberg" % "iceberg-spark-runtime-3.5_2.12" % Versions.iceberg,
  ("org.apache.iceberg" % "iceberg-aws" % Versions.iceberg)
    .exclude("com.fasterxml.jackson.core", "jackson-databind")
    .exclude("com.fasterxml.jackson.core", "jackson-core")
    .exclude("com.fasterxml.jackson.core", "jackson-annotations"),

  "org.apache.iceberg" % "iceberg-hive-runtime" % "1.7.2",
  "org.apache.hive" % "hive-metastore" % "4.0.1",

  "com.typesafe" % "config" % "1.4.3",
  "org.slf4j" % "slf4j-api" % Versions.slf4j,
  "org.apache.logging.log4j" % "log4j-api" % Versions.log4j,
  "org.apache.logging.log4j" % "log4j-core" % Versions.log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % Versions.log4j,

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
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % Versions.jackson
)
