package top.poools.coreproxy.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.poools.coreproxy.exception.FatalProcessorException;
import top.poools.coreproxy.exception.MessageProcessorException;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@SuppressWarnings("unused")
public class AuthorizeMessageProcessor extends AbstractMessageProcessor {

    public static final String METHOD_NAME = "mining.authorize";
    private static final Type TYPE = Type.CLIENT;


    public AuthorizeMessageProcessor(
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
        validateStratumMessageParams(stratumMessage);

        var userMiner = extractUserMiner(stratumMessage);
        if (userMiner == null) {
            throw new MessageProcessorException("Invalid authorize string in stratum message");
        }
        var user = userService.getUserByLogin(userMiner.getUserLogin());
        if (user == null) {
            throw new MessageProcessorException("Unauthorized user: " + userMiner.getUserLogin());
        }
        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new MessageProcessorException("User " + userMiner.getUserLogin() + " is blocked");
        }

        var miner = minerService.getMinerByNameAndUserId(userMiner.getMinerName(), user.getId());
        if (miner == null) {
            miner = minerService.registerMiner(
                    user.getId(),
                    userMiner.getMinerName(),
                    context.getMinerInfo(),
                    context.getPool().getId(),
                    context.getInetAddress());
            log.info("miner: {} has registered for user: {}", userMiner.getMinerName(), user.getLogin());
        } else {
            //if the miner has connected to the desired pool, update its inetAddress and reset connectAttempt to 0
            if (context.getPool().getId().equals(miner.getPoolId())) {
                miner = minerService.updateMiner(miner, context.getMinerInfo(), context.getInetAddress(), 0);
                log.info("miner: {} has updated for user: {}, set connectAttempt to 0",
                        userMiner.getMinerName(), user.getLogin());
            } else {
                //if the miner connected to a pool other than his own, update its inetAddress and increase connectAttempt by 1
                var connectAttempt = miner.getConnectAttempt() != null ?  miner.getConnectAttempt() + 1 : 1;
                if (connectAttempt == Integer.MAX_VALUE) {
                    log.warn("connectAttempt has reached its maximum value for miner: {} and will be reset to 0", miner.getName());
                    connectAttempt = 0;
                }
                miner = minerService.updateMiner(miner, context.getMinerInfo(), context.getInetAddress(), connectAttempt);
                log.info("miner: {} has updated for user: {}, set connectAttempt to {}",
                        userMiner.getMinerName(), user.getLogin(), connectAttempt);
                //if connectAttempt has exceeded the maximum number of attempts, add a random sleep
                waitIfNeed(connectAttempt);
                log.warn("wrong pool for miner {}. Miner will be reconnect to pool {}",
                        userMiner.getMinerName(), miner.getPoolId());
                //throw an exception so that the ASIC connects again, but with another pool
                throw new FatalProcessorException("Wrong pool for miner " + userMiner.getMinerName());
            }
        }

        String poolAccount = context.getPool().getAccount();
        stratumMessage.getParams().set(0, poolAccount + ".worker" + user.getId() + "m" + miner.getId());
        stratumMessage.getParams().set(1, "x");

        String convertedMessage = toJson(stratumMessage);

        context.setUser(user);
        context.setMiner(miner);
        log.debug("process method: [{}]", METHOD_NAME);
        return convertedMessage;
    }

    private void waitIfNeed(Integer connectAttempt) {
        if (connectAttempt > 3) {
            try {
                int sleepSeconds = ThreadLocalRandom.current().nextInt(1, 3);
                Thread.sleep(sleepSeconds * 1000L);
            } catch (InterruptedException e) {
                log.error("thread {} has interrupted", Thread.currentThread().getName(), e);
            }
        }
    }
}
