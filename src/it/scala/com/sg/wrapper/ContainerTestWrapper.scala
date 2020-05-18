package com.sg.wrapper

import com.dimafeng.testcontainers.{KafkaContainer, MySQLContainer, PostgreSQLContainer}
import io.debezium.testing.testcontainers.DebeziumContainer
import org.testcontainers.containers.Network

trait ContainerTestWrapper {
  lazy val network: Network = Network.newNetwork()

  lazy val kafkaContainer: KafkaContainer = KafkaContainer().configure(c => c.withNetwork(network))

  lazy val mySQLContainer: MySQLContainer = MySQLContainer(databaseName = "inventory").configure { c =>
    c.withNetwork(network)
    c.withConfigurationOverride("mysql-custom-conf")
  }

  lazy val debeziumContainer: DebeziumContainer = new DebeziumContainer("1.0").withKafka(kafkaContainer.container)
    .dependsOn(kafkaContainer.container)
    .dependsOn(mySQLContainer.container)
    .withNetwork(network)
    .withEnv("CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE", "true")
    .withEnv("CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE", "true")


  lazy val postgresContainer: PostgreSQLContainer = PostgreSQLContainer.apply(dockerImageNameOverride = "postgres:9.4", databaseName = "test", username = "test", password = "Test123").configure { c => c.addExposedPorts(5432) }

}
