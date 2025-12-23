package com.example.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import com.example.protocol.EventProto.Event;

import java.io.IOException;

public class KafkaEventConsumer {

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configuration from environment variables with defaults
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            bootstrapServers = "localhost:9092";
        }

        String topic = System.getenv("KAFKA_TOPIC");
        if (topic == null || topic.isEmpty()) {
            topic = "events";
        }

        // Configure Kafka source
        KafkaSource<Event> source = KafkaSource.<Event>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(topic)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new EventDeserializationSchema())
                .build();

        // Create data stream from Kafka
        DataStream<Event> events = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source"
        );

        // Process events
        events
                .map(event -> String.format(
                        "Event[id=%s, name=%s, timestamp=%d, data=%s]",
                        event.getId(),
                        event.getName(),
                        event.getTimestamp(),
                        event.getData()
                ))
                .print();

        // Execute the Flink job
        env.execute("Kafka Event Consumer");
    }

    /**
     * Deserialization schema for Protobuf Event messages
     */
    public static class EventDeserializationSchema extends AbstractDeserializationSchema<Event> {

        @Override
        public Event deserialize(byte[] message) throws IOException {
            try {
                return Event.parseFrom(message);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                // Log error and throw IOException to let Flink handle it
                System.err.println("Failed to deserialize Protobuf message: " + e.getMessage());
                throw new IOException("Invalid Protobuf message", e);
            }
        }
    }
}
