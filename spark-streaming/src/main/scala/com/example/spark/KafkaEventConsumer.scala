package com.example.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import com.example.protocol.EventProto.Event

object KafkaEventConsumer {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaEventConsumer")
      .getOrCreate()

    import spark.implicits._

    // Read from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "events")
      .option("startingOffsets", "earliest")
      .load()

    // Deserialize Protobuf messages
    val eventsDF = kafkaDF.select(
      col("key").cast("string").as("key"),
      deserializeEvent(col("value")).as("event")
    )

    // Expand the event struct
    val expandedDF = eventsDF.select(
      col("key"),
      col("event.id"),
      col("event.name"),
      col("event.timestamp"),
      col("event.data")
    )

    // Write to console for demonstration
    val query = expandedDF.writeStream
      .format("console")
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination()
  }

  // UDF to deserialize Protobuf Event
  // Returns tuple: (id: String, name: String, timestamp: Long, data: String)
  val deserializeEvent = udf((bytes: Array[Byte]) => {
    if (bytes != null) {
      val event = Event.parseFrom(bytes)
      (event.getId, event.getName, event.getTimestamp, event.getData)
    } else {
      ("", "", 0L, "")
    }
  })
}
