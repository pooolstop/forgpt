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
public class SetDifficultyMessageProcessor extends AbstractMessageProcessor {

    public static final List<String> METHOD_NAMES = Arrays.asList("mining.set_target", "mining.set_difficulty");
    private static final Type TYPE = Type.POOL;

    public SetDifficultyMessageProcessor(
            ObjectMapper objectMapper,
            UserService userService,
            MinerService minerService
    ) {
        super(objectMapper, userService, minerService);
    }

    @Override
    public boolean apply(Type type, StratumMessage stratumMessage) {
        var result = stratumMessage.getResult();
        return TYPE.equals(type) && result instanceof List<?>;
    }

    @Override
    public String process(StratumMessage stratumMessage, ClientContext context) {
        ((List<?>) stratumMessage.getResult()).stream()
                .filter(param -> param instanceof List)
                .flatMap(param -> ((List<?>) param).stream())
                .forEach(param -> {
                    if (param instanceof List paramList && paramList.size() >= 2) {
                        if (paramList.get(0) instanceof String methodName && METHOD_NAMES.contains(methodName)) {
                            if (paramList.get(1) instanceof String value) {
                                var difficulty = parseDifficulty(value);
                                context.setDifficulty(difficulty);
                                log.info("set difficulty: {}", difficulty);
                            }
                        }
                    }
                });
        String convertedMessage = toJson(stratumMessage);
        log.debug("process set difficulty for message: {}", stratumMessage);
        return convertedMessage;
    }

    private static Long parseDifficulty(String value) {
        try {
            return DifficultyUtils.parse(value, 16);
        } catch (Exception ex) {
            log.error("can not parse difficulty from value {} using 16 radix", value, ex);
            try {
                return DifficultyUtils.parse(value, 10);
            } catch (Exception e) {
                log.error("can not parse difficulty from value {} using 10 radix", value, e);
                return DEFAULT_DIFFICULTY;
            }
        }
    }
}