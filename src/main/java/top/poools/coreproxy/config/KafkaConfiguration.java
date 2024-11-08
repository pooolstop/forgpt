package top.poools.coreproxy.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import top.poools.coreproxy.properties.ShareKafkaProperties;
import top.poools.msapi.kafka.ShareEntity;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(ShareKafkaProperties.class)
public class KafkaConfiguration {

    @Bean
    public KafkaTemplate<String, ShareEntity> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(producerFactory(kafkaProperties));
    }

    @Bean
    public ProducerFactory<String, ShareEntity> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(getProducerProperties(kafkaProperties));
    }

    private Map<String, Object> getProducerProperties(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }
}
