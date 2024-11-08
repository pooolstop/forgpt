package top.poools.coreproxy.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubscribeMessageProcessor extends AbstractMessageProcessor {

    public static final String METHOD_NAME = "mining.subscribe";
    private static final Type TYPE = Type.CLIENT;

    public SubscribeMessageProcessor(
            ObjectMapper objectMapper,
            UserService userService,
            MinerService minerService
    ) {
        super(objectMapper, userService, minerService);
    }

    @Override
    public boolean apply(Type type, StratumMessage stratumMessage) {
        return TYPE.equals(type) && METHOD_NAME.equals(stratumMessage.getMethod());
    }

    @Override
    public String process(StratumMessage stratumMessage, ClientContext context) {
        var params = stratumMessage.getParams();
        log.debug("process method: [{}]", METHOD_NAME);
        if (params != null && !params.isEmpty()) {
            String minerInfo = params.get(0).toString();
            context.setMinerInfo(minerInfo);
            log.info("set miner info: {}", minerInfo);
            return toJson(stratumMessage);
        } else {
            log.debug("miner info is empty");
            return SKIP_RESULT;
        }
    }
}