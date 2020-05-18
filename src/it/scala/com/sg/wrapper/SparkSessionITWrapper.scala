package com.sg.wrapper

import org.apache.spark.sql.SparkSession

trait SparkSessionITWrapper {

  lazy val spark: SparkSession = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("spark test example")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")
    spark
  }

}
