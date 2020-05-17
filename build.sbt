name := "spark-streaming-with-debezium"

version := "0.1"
scalaVersion := "2.12.8"

val sparkVersion = "2.4.5"
val testcontainersScalaVersion = "0.37.0"

resolvers += "confluent" at "http://packages.confluent.io/maven/"
resolvers += Resolver.jcenterRepo
resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"


assemblyMergeStrategy in assembly := {
  case "META-INF/services/org.apache.spark.sql.sources.DataSourceRegister" => MergeStrategy.concat
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case "application.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

coverageEnabled.in(ThisBuild, IntegrationTest, test) := true

//skipping test cases during package
test in assembly := {}

lazy val server = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.kafka" % "kafka-clients" % "2.2.1",
  "org.postgresql" % "postgresql" % "42.2.5",
  "com.typesafe" % "config" % "1.4.0",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "org.scalatest" % "scalatest_2.12" % "3.0.1" % "it,test",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % IntegrationTest,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % IntegrationTest,
  "com.dimafeng" %% "testcontainers-scala-kafka" % testcontainersScalaVersion % IntegrationTest,
  "com.dimafeng" %% "testcontainers-scala-mysql" % testcontainersScalaVersion % IntegrationTest,
  "io.debezium" % "debezium-testing-testcontainers" % "1.0.3.Final" % IntegrationTest,
  "mysql" % "mysql-connector-java" % "5.1.49" % IntegrationTest
)


