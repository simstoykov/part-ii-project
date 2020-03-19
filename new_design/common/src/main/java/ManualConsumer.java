import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class ManualConsumer<K, V> {
    private final static Logger LOGGER = Logger.getLogger(ManualConsumer.class.getName());

    static final long DEFAULT_BLOCK_ON_POLL_MS = 10 * 1000; // TODO: Check

    List<KafkaConsumerObserver<K, V>> subscribers = new CopyOnWriteArrayList<>();
    protected Consumer<K, V> kafkaConsumer;
    private final long blockOnPollMs;

    ManualConsumer(Consumer<K, V> kafkaConsumer, long blockOnPollMs) {
        this.kafkaConsumer = kafkaConsumer;
        this.blockOnPollMs = blockOnPollMs;
    }

    ManualConsumer(Consumer<K, V> kafkaConsumer) {
        this.subscribers = new ArrayList<>();
        this.kafkaConsumer = kafkaConsumer;
        this.blockOnPollMs = DEFAULT_BLOCK_ON_POLL_MS;
    }

    public void subscribe(KafkaConsumerObserver<K, V> subscriber) {
        this.subscribers.add(subscriber);
    }

    ConsumerRecords<K, V> consumeRecords() {
        ConsumerRecords<K, V> consumerRecords = this.kafkaConsumer.poll(java.time.Duration.ofMillis(blockOnPollMs));
        this.kafkaConsumer.commitSync();
//        LOGGER.info("Consumed " + consumerRecords.count() + " records");
        return consumerRecords;
    }

    public void moveAllToLatest() {
        this.kafkaConsumer.poll(Duration.ofMillis(0)); // Kafka is Lazy

        LOGGER.info("Moving consumer to the latest offset");
        this.kafkaConsumer.seekToEnd(Collections.emptyList()); // All partitions
        LOGGER.info("Kafka consumer moved to offset " +
        this.kafkaConsumer.endOffsets(this.kafkaConsumer.assignment()));
    }

    public int consumeAvailableRecords() {
        ConsumerRecords<K, V> consumerRecords = this.consumeRecords();

        LOGGER.info("Consumed " + consumerRecords.count() + " records. Pinging the subscribers...");
        consumerRecords.forEach(record ->
            this.subscribers.forEach(subscriber -> subscriber.messageReceived(record)));

        LOGGER.info("Successfully consumed " + consumerRecords.count() + " records.");
        return consumerRecords.count();
    }

    void close() {
        this.kafkaConsumer.close();
    }
}