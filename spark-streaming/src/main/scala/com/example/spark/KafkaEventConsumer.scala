package com.example.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import com.example.protocol.EventProto.Event
import org.slf4j.LoggerFactory

object KafkaEventConsumer {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaEventConsumer")
      .getOrCreate()

    import spark.implicits._

    // Configuration from environment variables with defaults
    val bootstrapServers = sys.env.getOrElse("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    val topic = sys.env.getOrElse("KAFKA_TOPIC", "events")

    // Read from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("subscribe", topic)
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
  // Returns null on deserialization failure to allow pipeline to continue processing valid messages
  val deserializeEvent = udf((bytes: Array[Byte]) => {
    if (bytes != null) {
      try {
        val event = Event.parseFrom(bytes)
        (event.getId, event.getName, event.getTimestamp, event.getData)
      } catch {
        case e: com.google.protobuf.InvalidProtocolBufferException =>
          // Log error and return null to indicate deserialization failure
          logger.error(s"Failed to deserialize Protobuf message: ${e.getMessage}", e)
          null
      }
    } else {
      null
    }
  })
}
