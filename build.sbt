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

resolvers += "GitHub Package Registry" at "https://maven.pkg.github.com/DynamicYield/event-collection-schema"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.iceberg" % "iceberg-spark-runtime-3.5_2.12" % "1.4.3",
  "org.apache.hadoop" % "hadoop-client" % "3.3.6",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.6",
  "software.amazon.awssdk" % "s3" % "2.31.37",
  "software.amazon.awssdk" % "aws-sdk-java" % "2.31.37",
  "software.amazon.awssdk" % "sts" % "2.31.37",
  "com.dynamicyield" % "event-collection-schema" % "7.2.0",  // Java dependency
  "org.apache.spark" %% "spark-avro" % "3.5.0",  // For Avro support
  // Add Glue dependencies
  "org.apache.iceberg" % "iceberg-aws" % "1.4.3",
  "software.amazon.awssdk" % "glue" % "2.31.37",
  // Add Hive dependencies for local development
  "org.apache.iceberg" % "iceberg-hive-runtime" % "1.4.3",
  "org.apache.hive" % "hive-metastore" % "3.1.3",
  // Add TypeSafe Config
  "com.typesafe" % "config" % "1.4.3",
  // Testing dependencies
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.testcontainers" % "testcontainers" % "1.21.0" % Test,
  "io.minio" % "minio" % "8.5.17" % Test,
  "com.squareup.okhttp3" % "okhttp" % "4.12.0" % Test
)
