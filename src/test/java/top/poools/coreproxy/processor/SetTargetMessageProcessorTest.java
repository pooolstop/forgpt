package top.poools.coreproxy.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.util.DifficultyUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SetTargetMessageProcessorTest {

    @Spy
    private ObjectMapper objectMapper;
    @InjectMocks
    private SetTargetMessageProcessor messageProcessor;

    @ParameterizedTest
    @ValueSource(strings = {"mining.set_target", "mining.set_difficulty"})
    void shouldApplyProcessorIfMethodIsSetDifficulty(String method) throws JsonProcessingException {
        String message = "{\"id\":null,\"method\":\"" + method + "\",\"params\":[65536]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.POOL, stratumMessage);
        assertTrue(applyResult);
    }

    @Test
    void shouldNotApplyIfNullMethod() throws JsonProcessingException {
        String message = "{\"id\":14524,\"method\":null,\"error\":null,\"result\":\"result configure\",\"params\":null}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.POOL, stratumMessage);
        assertFalse(applyResult);
    }

    @Test
    void shouldSetDifficultyIfParamsHasDifficultyValue() throws JsonProcessingException {
        Long difficulty = 65536L;
        String message = "{\"id\":null,\"method\":\"mining.set_difficulty\",\"params\":[" + difficulty + "]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);
        ClientContext context = new ClientContext();

        messageProcessor.process(stratumMessage, context);

        assertEquals(difficulty, context.getDifficulty());
    }

    @Test
    void shouldSetDefaultDifficultyIfParamsHasNotValue() throws JsonProcessingException {
        String message = "{\"id\":null,\"method\":\"mining.set_difficulty\",\"params\":[null]}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);
        ClientContext context = new ClientContext();

        messageProcessor.process(stratumMessage, context);

        assertEquals(DifficultyUtils.DEFAULT_DIFFICULTY, context.getDifficulty());
    }
}