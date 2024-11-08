package top.poools.coreproxy.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.poools.coreproxy.model.Miner;
import top.poools.coreproxy.exception.MessageProcessorException;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMessageProcessor implements MessageProcessor {

    protected final ObjectMapper objectMapper;
    protected final UserService userService;
    protected final MinerService minerService;
    private static final Pattern USER_MINER_PATTERN = Pattern.compile("(.+?)\\.(.+?)");

    protected String toJson(StratumMessage stratumMessage) {
        try {
            return objectMapper.writeValueAsString(stratumMessage);
        } catch (Exception e) {
            throw new MessageProcessorException("Stratum message write error", e);
        }
    }

    protected void validateStratumMessageParams(StratumMessage stratumMessage) {
        var params = stratumMessage.getParams();
        if (params == null || params.isEmpty()) {
            throw new MessageProcessorException("Params are empty in stratumMessage");
        }
    }

    protected UserMiner extractUserMiner(StratumMessage stratumMessage) {
        String userAndWorker = stratumMessage.getParams().get(0).toString();
        var matcher = USER_MINER_PATTERN.matcher(userAndWorker);
        if (matcher.matches()) {
            return new UserMiner()
                    .setUserLogin(matcher.group(1))
                    .setMinerName(userAndWorker);
                    //todo: remove user login from miner name!
                    //.setMinerName(matcher.group(2));
        }
        return null;
    }

    protected Miner findMinerByName(Set<Miner> minerSet, String minerName) {
        if (minerSet == null || minerSet.isEmpty()) {
            return null;
        }
        return minerSet.stream()
                .filter(miner -> miner.getName().equals(minerName))
                .findAny()
                .orElse(null);
    }

    @Data
    @Accessors(chain = true)
    public static class UserMiner {
        private String userLogin;
        private String minerName;
    }
}