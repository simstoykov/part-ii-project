import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.logging.Logger;

public class ManualConsumer<K, V> extends SubscribableConsumer<K, V> {
    private static final Logger LOGGER = Logger.getLogger(ManualConsumer.class.getName());

    public ManualConsumer(Consumer<K, V> kafkaConsumer) {
        super(kafkaConsumer);
    }

    public int consumeAvailableRecords() {
        ConsumerRecords<K, V> consumerRecords = this.consumeRecords();

        LOGGER.info("Consumed " + consumerRecords.count() + " records. Pinging the subscribers...");
        consumerRecords.forEach(record ->
            this.subscribers.forEach(subscriber -> subscriber.messageReceived(record)));

        LOGGER.info("Successfully consumed " + consumerRecords.count() + " records.");
        return consumerRecords.count();
    }

    public void moveAllToLatest() {
        this.kafkaConsumer.seekToEnd(this.kafkaConsumer.assignment());
    }
}