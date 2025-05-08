error id: 
file://<WORKSPACE>/build.sbt
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 520
uri: file://<WORKSPACE>/build.sbt
text:
```scala
name := "spark-iceberg-example"
version := "0.1.0"
scalaVersion := "2.12.18"

resolvers += "GitHub Packages" at "https://maven.pkg.github.com/DynamicYield/event-collection-schema"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.iceberg" % "iceberg-spark-runtime-3.5_2.12" % "1.4.3",
  "org.apache.hadoop" % "hadoop-client" % "3.3.6",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.6",
  "software.amazon.awssdk" % "s3" % "2.31.37",
  "software.amazon.awssdk" % "aws-sdk-java" % "2@@.24.17",
  "software.amazon.awssdk" % "sts" % "2.24.17",
  "com.dynamicyield" % "event-collection-schema" % "7.2.0"  // Java dependency
)

// Add Spark and Iceberg configurations
Compile / run / fork := true
Compile / run / javaOptions ++= Seq(
  "-Dspark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions",
  "-Dspark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog",
  "-Dspark.sql.catalog.spark_catalog.type=hive",
  // S3/MinIO configurations
  "-Dspark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem",
  "-Dspark.hadoop.fs.s3a.aws.credentials.provider=org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider",
  "-Dspark.hadoop.fs.s3a.endpoint=http://localhost:9000",  // Default MinIO endpoint
  "-Dspark.hadoop.fs.s3a.path.style.access=true"  // Required for MinIO
) 
```


#### Short summary: 

empty definition using pc, found symbol in pc: 