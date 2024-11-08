package top.poools.coreproxy.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.poools.coreproxy.exception.FatalProcessorException;
import top.poools.coreproxy.exception.MessageProcessorException;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.model.Share;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Slf4j
@Component
@SuppressWarnings("unused")
public class SubmitMessageProcessor extends AbstractMessageProcessor {

    public static final String METHOD_NAME = "mining.submit";
    private static final Type TYPE = Type.CLIENT;

    public SubmitMessageProcessor(
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
        LocalDateTime submittedTime = LocalDateTime.now();
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
            if (!context.getPool().getId().equals(miner.getPoolId())) {
                log.warn("wrong pool for miner {}. Miner will be reconnect to pool {}",
                        userMiner.getMinerName(), miner.getPoolId());
                throw new FatalProcessorException("Wrong pool for miner " + userMiner.getMinerName());
            }
        }

        String poolAccount = context.getPool().getAccount();
        stratumMessage.getParams().set(0, poolAccount + "." + user.getId() + "_" + miner.getId());

        String convertedMessage = toJson(stratumMessage);
        Share share = new Share()
                .setMessageId(stratumMessage.getId())
                .setSubmittedTime(submittedTime)
                .setDifficulty(context.getDifficulty())
                .setIsSubmit(false)
                .setUserId(user.getId())
                .setMinerId(miner.getId())
                .setPoolId(context.getPool().getId());
        context.addShare(share);
        context.setUser(user);
        context.setMiner(miner);
        context.setLastUpdated(LocalDateTime.now());
        log.info("share sent [messageId = {}]", share.getMessageId());
        log.debug("share: {} has added to context", share);

        log.debug("process method: [{}]", METHOD_NAME);
        return convertedMessage;
    }
}