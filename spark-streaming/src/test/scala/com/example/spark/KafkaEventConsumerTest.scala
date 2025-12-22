package com.example.spark

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.protocol.EventProto.Event
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

class KafkaEventConsumerTest extends AnyFlatSpec with Matchers {

  "Event deserialization" should "correctly parse Protobuf messages" in {
    val spark = SparkSession.builder()
      .appName("TestApp")
      .master("local[*]")
      .getOrCreate()

    try {
      import spark.implicits._

      // Create a test event
      val event = Event.newBuilder()
        .setId("test-123")
        .setName("TestEvent")
        .setTimestamp(System.currentTimeMillis())
        .setData("test data")
        .build()

      // Serialize to bytes
      val bytes = event.toByteArray

      // Create DataFrame with serialized data
      val df = Seq(bytes).toDF("value")

      // Apply deserialization UDF
      val deserializedDF = df.select(
        KafkaEventConsumer.deserializeEvent(col("value")).as("event")
      )

      // Collect results
      val result = deserializedDF.collect()

      result should have length 1
      val row = result.head
      val eventRow = row.getStruct(0)
      
      // Tuple field indices: 0=id, 1=name, 2=timestamp, 3=data
      eventRow.getAs[String](0) shouldBe "test-123"
      eventRow.getAs[String](1) shouldBe "TestEvent"
      eventRow.getAs[Long](2) shouldBe event.getTimestamp
      eventRow.getAs[String](3) shouldBe "test data"
    } finally {
      spark.stop()
    }
  }

  it should "handle null bytes gracefully" in {
    val spark = SparkSession.builder()
      .appName("TestApp")
      .master("local[*]")
      .getOrCreate()

    try {
      import spark.implicits._

      val df = Seq(null.asInstanceOf[Array[Byte]]).toDF("value")

      val deserializedDF = df.select(
        KafkaEventConsumer.deserializeEvent(col("value")).as("event")
      )

      val result = deserializedDF.collect()

      result should have length 1
      val row = result.head
      val eventRow = row.getStruct(0)
      
      // Tuple field indices: 0=id, 1=name, 2=timestamp, 3=data
      eventRow.getAs[String](0) shouldBe ""
      eventRow.getAs[String](1) shouldBe ""
      eventRow.getAs[Long](2) shouldBe 0L
      eventRow.getAs[String](3) shouldBe ""
    } finally {
      spark.stop()
    }
  }

  it should "deserialize multiple events" in {
    val spark = SparkSession.builder()
      .appName("TestApp")
      .master("local[*]")
      .getOrCreate()

    try {
      import spark.implicits._

      val events = (1 to 5).map { i =>
        Event.newBuilder()
          .setId(s"event-$i")
          .setName(s"Event$i")
          .setTimestamp(i.toLong)
          .setData(s"data-$i")
          .build()
          .toByteArray
      }

      val df = events.toDF("value")

      val deserializedDF = df.select(
        KafkaEventConsumer.deserializeEvent(col("value")).as("event")
      )

      val results = deserializedDF.collect()

      results should have length 5

      // Tuple field indices: 0=id, 1=name, 2=timestamp, 3=data
      results.zipWithIndex.foreach { case (row, idx) =>
        val i = idx + 1
        val eventRow = row.getStruct(0)
        eventRow.getAs[String](0) shouldBe s"event-$i"
        eventRow.getAs[String](1) shouldBe s"Event$i"
        eventRow.getAs[Long](2) shouldBe i.toLong
        eventRow.getAs[String](3) shouldBe s"data-$i"
      }
    } finally {
      spark.stop()
    }
  }
}
