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
```

This will:
- Compile the Protobuf schema
- Generate Java and Scala classes
- Run round-trip serialization tests

### Spark Streaming Module (Maven)

```bash
cd spark-streaming
mvn clean compile
mvn test
```

This will:
- Generate Java classes from Protobuf schema
- Compile the Spark streaming application
- Run tests for Kafka message deserialization

### Flink Streaming Module (Maven)

```bash
cd flink-streaming
mvn clean compile
mvn test
```

This will:
- Generate Java classes from Protobuf schema
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

The shared schema is defined in `protocol/src/main/protobuf/event.proto`:

```protobuf
message Event {
  string id = 1;
  string name = 2;
  int64 timestamp = 3;
  string data = 4;
}
```

**Note**: The schema file is duplicated in each module (protocol, spark-streaming, and flink-streaming) 
because each module uses different build systems and code generation tools:
- Protocol module uses SBT with ScalaPB for Scala code generation
- Spark and Flink modules use Maven with protobuf-maven-plugin for Java code generation

This approach ensures each module can independently generate and test its Protobuf bindings 
without cross-module dependencies, which is important for microservices architectures.

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

Both streaming applications expect:
- Kafka brokers at `localhost:9092`
- Topic name: `events`
- Messages serialized as Protobuf `Event` objects

## License

See LICENSE file for details.
