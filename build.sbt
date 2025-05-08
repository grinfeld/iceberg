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

resolvers += "GitHub Packages" at "https://maven.pkg.github.com/DynamicYield/event-collection-schema"

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
  "com.typesafe" % "config" % "1.4.3"
)

// Add Spark and Iceberg configurations
Compile / run / fork := true
Compile / run / javaOptions ++= Seq(
  "-Dspark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions",
  "-Dspark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog",
  "-Dspark.sql.catalog.spark_catalog.type=hive",
  "-Dspark.sql.catalog.spark_catalog.warehouse=s3a://your-warehouse-bucket/warehouse",
  // Local development: Use Hive catalog instead of Glue
  "-Dspark.sql.catalog.spark_catalog.catalog-impl=org.apache.iceberg.hive.HiveCatalog",
  "-Dspark.sql.catalog.spark_catalog.uri=thrift://localhost:9083",
  "-Dspark.sql.catalog.spark_catalog.clients=1",
  "-Dspark.sql.catalog.spark_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO",
  "-Dspark.sql.catalog.spark_catalog.s3.endpoint=http://localhost:9000",  // For local MinIO testing
  "-Dspark.sql.catalog.spark_catalog.s3.access-key=minioadmin",
  "-Dspark.sql.catalog.spark_catalog.s3.secret-key=minioadmin",
  // S3/MinIO configurations
  "-Dspark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem",
  "-Dspark.hadoop.fs.s3a.aws.credentials.provider=org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider",
  "-Dspark.hadoop.fs.s3a.endpoint=http://localhost:9000",  // Default MinIO endpoint
  "-Dspark.hadoop.fs.s3a.path.style.access=true"  // Required for MinIO
) 