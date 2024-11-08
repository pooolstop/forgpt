package top.poools.coreproxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import top.poools.coreproxy.exception.ServiceException;
import top.poools.coreproxy.mapper.ShareMapper;
import top.poools.coreproxy.model.Share;
import top.poools.coreproxy.properties.ShareKafkaProperties;
import top.poools.msapi.kafka.ShareEntity;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareKafkaService {

    private final KafkaTemplate<String, ShareEntity> kafkaTemplate;
    private final ShareMapper shareMapper;
    private final ShareKafkaProperties shareKafkaProperties;

    public void send(Share share) {
        try {
            var topic = shareKafkaProperties.getTopic();
            Optional.ofNullable(share)
                    .map(shareMapper::toKafka)
                    .map(s -> kafkaTemplate.send(topic, s))
                    .orElseThrow(() -> new NullPointerException("Share is null"));
            log.debug("share: {} has sent to kafka topic: {}", share, topic);
        } catch (Exception ex) {
            log.error("share kafka service error: ", ex);
            throw new ServiceException("Sending share error", ex);
        }
    }
}
