package com.sg.job.streaming

import java.sql.{Connection, DriverManager, Statement}
import java.time.LocalDateTime

import com.dimafeng.testcontainers.{ForAllTestContainer, MultipleContainers}
import com.sg.wrapper.{ContainerTestWrapper, SparkSessionITWrapper}
import io.debezium.testing.testcontainers.ConnectorConfiguration
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.slf4j.{Logger, LoggerFactory}

/**
 * A Integration Test for [[StreamingJob]]
 */
class StreamingIT extends FlatSpec with ForAllTestContainer with BeforeAndAfterAll with ContainerTestWrapper with SparkSessionITWrapper {

  val sparkTest: SparkSession = spark
  override val container: MultipleContainers = MultipleContainers(kafkaContainer, postgresContainer, mySQLContainer)
  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)

  it must "integration test for streaming job" in {

    logger.info("Kafka-Connect is ready to send events...")
    createBinLogDBRecords(mySQLContainer.driverClassName, mySQLContainer.jdbcUrl, mySQLContainer.username, mySQLContainer.password)

    logger.info("launching the job...")

    val checkpoint = new java.io.File(".").getCanonicalPath + "/checkpoint/it" + LocalDateTime.now()
    val kafkaReaderConfig = KafkaReaderConfig("localhost:29092", "dbserver1.inventory.orders")
    val jobcConfig = JDBCConfig(url = postgresContainer.jdbcUrl)

    new StreamingJobExecutor(sparkTest, kafkaReaderConfig, checkpoint, jobcConfig).execute()

    logger.info("Assert the records")
    Class.forName(postgresContainer.driverClassName)
    val connection: Connection = DriverManager.getConnection(postgresContainer.jdbcUrl, postgresContainer.username, postgresContainer.password)
    connection.setAutoCommit(true)
    connection.setSchema("test")
    val statement: Statement = connection.createStatement()

    val sql = "select * from public.orders_it"
    val rs = statement.executeQuery(sql)
    assert(rs != null)
    try {
      while (rs.next()) {
        //Put the logic for assertion
        println(rs.getString("key"), rs.getString("value"), rs.getString("topic"))
      }
    } finally {
      rs.close()
    }
  }

  override def beforeAll() {
    super.beforeAll()
    debeziumContainer.start()
    createSnapshotDBRecords(mySQLContainer.driverClassName, mySQLContainer.jdbcUrl, mySQLContainer.username, mySQLContainer.password)

    val connector = ConnectorConfiguration.forJdbcContainer(mySQLContainer.container)
      .`with`("database.server.name", "dbserver1")
      .`with`("connector.class", "io.debezium.connector.mysql.MySqlConnector")
      .`with`("database.whitelist", "inventory")
      .`with`("database.user", "root")
      .`with`("database.password", "test")
      .`with`("database.hostname", mySQLContainer.networkAliases.head)
      .`with`("database.server.id", "123")
      .`with`("database.port", "3306")
      .`with`("name", "my-connector")
      .`with`("database.history.kafka.bootstrap.servers", s"${kafkaContainer.networkAliases.head}:9092")
      .`with`("database.history.kafka.topic", "schema-changes.inventory")

    logger.info("Trying to connect to Kafka-Connect")
    debeziumContainer.registerConnector("my-connector", connector)

    //Giving time for kafka-connect to settle down.
    Thread.sleep(30000)

    //A way out of stopping the streaming job
    val runnable: Runnable = () =>
      try {
        Thread.sleep(60000)
        logger.info("Stopping the job...")
        sparkTest.streams.active.foreach(q => q.stop())
        sparkTest.sparkContext.stop()
        sparkTest.stop()
      } catch {
        case e: InterruptedException => e.printStackTrace()
      }
    val thread = new Thread(runnable)
    thread.start()
  }

  override def afterAll() {
    sparkTest.stop()
    debeziumContainer.stop()
    super.afterAll()
  }

  /**
   * DB changes for snapshot mode
   *
   */
  def createSnapshotDBRecords(driverClassName: String, jdbcUrl: String, username: String, password: String): Unit = {
    Class.forName(driverClassName)
    val connection: Connection = DriverManager.getConnection(jdbcUrl, username, password)
    val statement: Statement = connection.createStatement()
    statement.execute("create table inventory.orders (id int not null, title varchar(255), primary key (id))")
    statement.execute("insert into inventory.orders values (1, 'Learn ADC')")
    statement.execute("insert into inventory.orders values (2, 'Learn BDC')")
    statement.closeOnCompletion()
    connection.close()

  }
  /**
   * DB change for binlog mode
   *
   */
  def createBinLogDBRecords(driverClassName: String, jdbcUrl: String, username: String, password: String): Unit = {
    Class.forName(driverClassName)
    val connection: Connection = DriverManager.getConnection(jdbcUrl, username, password)
    val statement: Statement = connection.createStatement()
    statement.execute("insert into inventory.orders values (3, 'Learn CDC')")
    statement.execute("insert into inventory.orders values (4, 'Learn DDC')")
    statement.execute("update inventory.orders set title = 'Learn EDC' where id = 4")
    statement.execute("delete from inventory.orders where id = 3")
    statement.closeOnCompletion()
    connection.close()
  }

}
