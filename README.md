# sturdy-goggles
Kafka Streaming with Protobuf Schema

This project demonstrates Kafka streaming between Spark and Flink using a shared Protobuf schema with Kafka serialization.

## Project Structure

The project consists of three modules:

1. **protocol** - SBT-based module that owns the Protobuf schema and generates Java/Scala classes
2. **spark-streaming** - Maven-based Spark 3.5.3 streaming application that consumes Kafka messages
3. **flink-streaming** - Maven-based Flink 2.0.0 streaming application that consumes Kafka messages

## Prerequisites

- Java 17
- SBT 1.9.7 (for protocol module)
- Maven 3.6+ (for Spark and Flink modules)
- Kafka cluster (for running the streaming applications)

## Building the Project

### Protocol Module (SBT)

```bash
cd protocol
sbt compile
sbt test
sbt publishM2  # Publish to local Maven repository for use by other modules
```

This will:
- Compile the Protobuf schema
- Generate both Java and Scala classes
- Run round-trip serialization tests
- Publish the JAR to local Maven repository

### Spark Streaming Module (Maven)

```bash
cd spark-streaming
mvn clean compile
mvn test
```

This will:
- Use Protobuf classes from the protocol module dependency
- Compile the Spark streaming application
- Run tests for Kafka message deserialization

### Flink Streaming Module (Maven)

```bash
cd flink-streaming
mvn clean compile
mvn test
```

This will:
- Use Protobuf classes from the protocol module dependency
- Compile the Flink streaming application
- Run tests for Kafka message deserialization

## Running the Applications

### Spark Streaming App

```bash
cd spark-streaming
mvn package
spark-submit \
  --class com.example.spark.KafkaEventConsumer \
  --master local[*] \
  target/spark-streaming-1.0-SNAPSHOT.jar
```

### Flink Streaming App

```bash
cd flink-streaming
mvn package
flink run target/flink-streaming-1.0-SNAPSHOT.jar
```

## Protobuf Schema

The shared schema is defined in `protocol/src/main/proto/event.proto`:

```protobuf
message Event {
  string id = 1;
  string name = 2;
  int64 timestamp = 3;
  string data = 4;
}
```

**Architecture**: The protocol module serves as the single source of truth for all Protobuf definitions:
- The protocol module generates both Java and Scala classes from the proto files
- Spark and Flink modules depend on the protocol module's published JAR
- This eliminates schema duplication and ensures consistency across all modules
- The protocol module publishes to local Maven/Ivy repositories for use by other modules

All modules use the standard `src/main/proto` directory for consistency.

## Error Handling Strategy

The Spark and Flink modules use different error handling strategies for malformed Protobuf data:

### Spark Strategy: Continue Processing
- **Behavior**: Returns `null` when deserialization fails
- **Impact**: Pipeline continues processing, skipping malformed messages
- **Use case**: Best when data quality issues are expected and losing some messages is acceptable
- **Logging**: Errors logged via SLF4J at ERROR level

### Flink Strategy: Fail Fast
- **Behavior**: Throws `IOException` when deserialization fails
- **Impact**: Job will fail or restart based on Flink's configured restart strategy
- **Use case**: Best when data integrity is critical and corruption should halt processing
- **Logging**: Errors logged via SLF4J at ERROR level

This intentional difference allows each framework to handle failures according to its operational model. Choose the approach that best fits your requirements for data quality vs. availability.

## Testing

All modules include comprehensive tests:

- **Protocol module**: Tests for Protobuf schema round-trip serialization
- **Spark module**: Tests for deserializing Protobuf messages from Kafka
- **Flink module**: Tests for deserializing Protobuf messages from Kafka

Run all tests:

```bash
# Protocol tests
cd protocol && sbt test

# Spark tests
cd spark-streaming && mvn test

# Flink tests
cd flink-streaming && mvn test
```

## Kafka Configuration

Both streaming applications support configuration via environment variables:

### Environment Variables
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses (default: `localhost:9092`)
- `KAFKA_TOPIC`: Topic name to consume from (default: `events`)

### Message Format
Messages should be serialized as Protobuf `Event` objects using the shared schema.

## License

See LICENSE file for details.
