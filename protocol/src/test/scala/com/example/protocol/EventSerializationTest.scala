package com.example.protocol

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.protocol.event.Event

class EventSerializationTest extends AnyFlatSpec with Matchers {

  "Event" should "serialize and deserialize correctly" in {
    val event = Event(
      id = "event-123",
      name = "TestEvent",
      timestamp = System.currentTimeMillis(),
      data = "test data"
    )

    // Serialize to bytes
    val bytes = event.toByteArray

    // Deserialize from bytes
    val deserializedEvent = Event.parseFrom(bytes)

    // Verify round-trip
    deserializedEvent.id shouldBe event.id
    deserializedEvent.name shouldBe event.name
    deserializedEvent.timestamp shouldBe event.timestamp
    deserializedEvent.data shouldBe event.data
  }

  it should "handle empty data" in {
    val event = Event(
      id = "event-456",
      name = "EmptyEvent",
      timestamp = 0L,
      data = ""
    )

    val bytes = event.toByteArray
    val deserializedEvent = Event.parseFrom(bytes)

    deserializedEvent shouldBe event
  }

  it should "preserve all fields during serialization" in {
    val event = Event(
      id = "event-789",
      name = "ComplexEvent",
      timestamp = 1234567890L,
      data = "Complex data with special chars: !@#$%^&*()"
    )

    val bytes = event.toByteArray
    val deserializedEvent = Event.parseFrom(bytes)

    deserializedEvent.id shouldBe event.id
    deserializedEvent.name shouldBe event.name
    deserializedEvent.timestamp shouldBe event.timestamp
    deserializedEvent.data shouldBe event.data
  }
}
