package top.poools.coreproxy.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.UserService;
import top.poools.coreproxy.util.DifficultyUtils;

import java.util.Arrays;
import java.util.List;

import static top.poools.coreproxy.util.DifficultyUtils.DEFAULT_DIFFICULTY;

@Slf4j
@Component
public class SetTargetMessageProcessor extends AbstractMessageProcessor {

    public static final List<String> METHOD_NAMES = Arrays.asList("mining.set_target", "mining.set_difficulty");
    private static final Type TYPE = Type.POOL;

    public SetTargetMessageProcessor(
            ObjectMapper objectMapper,
            UserService userService,
            MinerService minerService
    ) {
        super(objectMapper, userService, minerService);
    }

    @Override
    public boolean apply(Type type, StratumMessage stratumMessage) {
        return TYPE.equals(type) && METHOD_NAMES.contains(stratumMessage.getMethod());
    }

    @Override
    public String process(StratumMessage stratumMessage, ClientContext context) {
        var params = stratumMessage.getParams();
        if (params != null && !params.isEmpty()) {
            Object difficultyParam = stratumMessage.getParams().get(0);
            Long difficulty = parseDifficulty(difficultyParam);
            context.setDifficulty(difficulty);
            log.info("set difficulty: {}", difficulty);
        } else {
            log.warn("difficulty is empty");
        }
        String convertedMessage = toJson(stratumMessage);
        log.debug("process method: [{}]", stratumMessage.getMethod());
        return convertedMessage;
    }

    private Long parseDifficulty(Object difficulty) {
        if (difficulty == null) {
            log.warn("difficulty is null. Set difficulty to default");
            return DEFAULT_DIFFICULTY;
        }
        try {
            return DifficultyUtils.parse(difficulty.toString(), 10);
        } catch (Exception e) {
            log.error("can not parse difficulty from value {} using 10 radix", difficulty, e);
            return DEFAULT_DIFFICULTY;
        }
    }
}