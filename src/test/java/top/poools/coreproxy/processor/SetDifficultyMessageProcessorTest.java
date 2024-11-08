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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SetDifficultyMessageProcessorTest {

    @Spy
    private ObjectMapper objectMapper;
    @InjectMocks
    private SetDifficultyMessageProcessor messageProcessor;

    @Test
    void shouldApplyProcessorIfResultIsList() throws JsonProcessingException {
        String message = "{\"error\":null,\"id\":149,\"result\":[[[\"mining.notify\",\"0000901b1\"],[\"mining.set_difficulty\",\"0000901b2\"]],\"0000901b\",8]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.POOL, stratumMessage);
        assertTrue(applyResult);
    }

    @Test
    void shouldNotApplyProcessorIfResultIsNull() throws JsonProcessingException {
        String message = "{\"error\":null,\"id\":149,\"result\":null}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.POOL, stratumMessage);
        assertFalse(applyResult);
    }

    @Test
    void shouldSetDifficultyIfResultIsList() throws JsonProcessingException {
        Long expectedDifficulty = Long.parseLong("0000901b2", 16);
        String message = "{\"error\":null,\"id\":149,\"result\":[[[\"mining.notify\",\"0000901b1\"],[\"mining.set_difficulty\",\"0000901b2\"]],\"0000901b\",8]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);
        ClientContext context = new ClientContext();

        messageProcessor.process(stratumMessage, context);

        assertEquals(expectedDifficulty, context.getDifficulty());
    }
}
