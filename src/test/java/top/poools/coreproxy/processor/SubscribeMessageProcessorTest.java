package top.poools.coreproxy.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SubscribeMessageProcessorTest {

    @Spy
    private ObjectMapper objectMapper;
    @InjectMocks
    private SubscribeMessageProcessor messageProcessor;

    @Test
    void shouldApplyProcessorIfMethodIsSubscribe() throws JsonProcessingException {
        String message = "{\"id\": 149, \"method\": \"mining.subscribe\", \"params\": [\"Antminer S19j Pro/Fri Oct 15 11:25:11 CST 2021\", \"1b9000001\"]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.CLIENT, stratumMessage);
        assertTrue(applyResult);
    }

    @Test
    void shouldSetMinerInfoIfParamsNotEmpty() throws JsonProcessingException {
        String minerInfo = "Antminer S19j Pro/Fri Oct 15 11:25:11 CST 2021";
        String message = "{\"id\": 149, \"method\": \"mining.subscribe\", \"params\": [\"" + minerInfo + "\", \"1b9000001\"]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);
        ClientContext context = new ClientContext();

        messageProcessor.process(stratumMessage, context);

        assertEquals(minerInfo, context.getMinerInfo());
    }

    @Test
    void shouldNotSetDifficultyIfParamsAreEmpty() throws JsonProcessingException {
        String message = "{\"id\": 14545, \"method\": \"mining.subscribe\", \"params\": []}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);
        ClientContext context = new ClientContext();

        messageProcessor.process(stratumMessage, context);

        assertNull(context.getMinerInfo());
    }
}