package top.poools.coreproxy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "share.kafka")
public class ShareKafkaProperties {
    private String topic;
}