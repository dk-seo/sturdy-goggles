package com.example.flink;

import com.example.protocol.EventProto.Event;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class KafkaEventConsumerTest {

    @Test
    public void testEventDeserialization() throws IOException {
        // Create a test event
        Event event = Event.newBuilder()
                .setId("test-123")
                .setName("TestEvent")
                .setTimestamp(System.currentTimeMillis())
                .setData("test data")
                .build();

        // Serialize to bytes
        byte[] bytes = event.toByteArray();

        // Deserialize using the schema
        KafkaEventConsumer.EventDeserializationSchema schema =
                new KafkaEventConsumer.EventDeserializationSchema();
        Event deserializedEvent = schema.deserialize(bytes);

        // Verify
        assertNotNull(deserializedEvent);
        assertEquals("test-123", deserializedEvent.getId());
        assertEquals("TestEvent", deserializedEvent.getName());
        assertEquals(event.getTimestamp(), deserializedEvent.getTimestamp());
        assertEquals("test data", deserializedEvent.getData());
    }

    @Test
    public void testEventDeserializationWithEmptyData() throws IOException {
        Event event = Event.newBuilder()
                .setId("test-456")
                .setName("EmptyEvent")
                .setTimestamp(0L)
                .setData("")
                .build();

        byte[] bytes = event.toByteArray();

        KafkaEventConsumer.EventDeserializationSchema schema =
                new KafkaEventConsumer.EventDeserializationSchema();
        Event deserializedEvent = schema.deserialize(bytes);

        assertNotNull(deserializedEvent);
        assertEquals("test-456", deserializedEvent.getId());
        assertEquals("EmptyEvent", deserializedEvent.getName());
        assertEquals(0L, deserializedEvent.getTimestamp());
        assertEquals("", deserializedEvent.getData());
    }

    @Test
    public void testEventDeserializationWithSpecialChars() throws IOException {
        Event event = Event.newBuilder()
                .setId("test-789")
                .setName("SpecialEvent")
                .setTimestamp(1234567890L)
                .setData("Special chars: !@#$%^&*()")
                .build();

        byte[] bytes = event.toByteArray();

        KafkaEventConsumer.EventDeserializationSchema schema =
                new KafkaEventConsumer.EventDeserializationSchema();
        Event deserializedEvent = schema.deserialize(bytes);

        assertNotNull(deserializedEvent);
        assertEquals("test-789", deserializedEvent.getId());
        assertEquals("SpecialEvent", deserializedEvent.getName());
        assertEquals(1234567890L, deserializedEvent.getTimestamp());
        assertEquals("Special chars: !@#$%^&*()", deserializedEvent.getData());
    }

    @Test
    public void testMultipleEventsSerialization() throws IOException {
        KafkaEventConsumer.EventDeserializationSchema schema =
                new KafkaEventConsumer.EventDeserializationSchema();

        for (int i = 1; i <= 5; i++) {
            Event event = Event.newBuilder()
                    .setId("event-" + i)
                    .setName("Event" + i)
                    .setTimestamp((long) i)
                    .setData("data-" + i)
                    .build();

            byte[] bytes = event.toByteArray();
            Event deserializedEvent = schema.deserialize(bytes);

            assertNotNull(deserializedEvent);
            assertEquals("event-" + i, deserializedEvent.getId());
            assertEquals("Event" + i, deserializedEvent.getName());
            assertEquals((long) i, deserializedEvent.getTimestamp());
            assertEquals("data-" + i, deserializedEvent.getData());
        }
    }

    @Test(expected = IOException.class)
    public void testMalformedProtobufData() throws IOException {
        // Create malformed data (random bytes that aren't valid Protobuf)
        byte[] malformedBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        KafkaEventConsumer.EventDeserializationSchema schema =
                new KafkaEventConsumer.EventDeserializationSchema();
        
        // This should throw IOException due to invalid Protobuf data
        schema.deserialize(malformedBytes);
    }
}
