package top.poools.coreproxy.processor;

import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;

public interface MessageProcessor {

    String SKIP_RESULT = "skip";

    enum Type {
        CLIENT,
        POOL
    }

    boolean apply(Type type, StratumMessage stratumMessage);

    String process(StratumMessage stratumMessage, ClientContext context);
}