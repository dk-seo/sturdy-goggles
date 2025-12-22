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

        // Configure Kafka source
        KafkaSource<Event> source = KafkaSource.<Event>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("events")
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
            return Event.parseFrom(message);
        }
    }
}
