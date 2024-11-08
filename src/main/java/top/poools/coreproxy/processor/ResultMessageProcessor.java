package top.poools.coreproxy.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.Share;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.ShareKafkaService;
import top.poools.coreproxy.service.UserService;

@Slf4j
@Component
public class ResultMessageProcessor extends AbstractMessageProcessor {

    private static final Type TYPE = Type.POOL;
    private final ShareKafkaService shareKafkaService;

    public ResultMessageProcessor(
            ObjectMapper objectMapper,
            UserService userService,
            MinerService minerService,
            ShareKafkaService shareKafkaService
    ) {
        super(objectMapper, userService, minerService);
        this.shareKafkaService = shareKafkaService;
    }

    @Override
    public boolean apply(Type type, StratumMessage stratumMessage) {
        //todo: exclude responses from method 'mining.authorize', because the have the same boolean result
        var result = stratumMessage.getResult();
        return TYPE.equals(type) &&
                (result instanceof Boolean || (result == null && stratumMessage.getError() != null));
    }

    @Override
    public String process(StratumMessage stratumMessage, ClientContext context) {
        Integer messageId = stratumMessage.getId();
        Share share = context.getShare(messageId);
        if (share != null) {
            boolean isSubmitted = Boolean.TRUE.equals(stratumMessage.getResult());
            share.setIsSubmit(isSubmitted);
            shareKafkaService.send(share);
            context.removeShare(messageId);
            log.debug("share with messageId: {} has removed from context", messageId);
            if (isSubmitted) {
                log.info("share submitted [messageId = {}]", messageId);
            } else {
                log.info("share rejected [messageId = {}]", messageId);
            }
        }
        //todo: temporary comment log, because this processor apply 'mining.authorize' responses too
        //else {
        //    log.warn("can not find share with messageId: {}", messageId);
        //}

        String convertedMessage = toJson(stratumMessage);
        log.debug("process result for message: {}", stratumMessage);
        return convertedMessage;
    }
}