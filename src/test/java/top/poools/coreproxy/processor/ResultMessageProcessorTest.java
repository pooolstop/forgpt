package top.poools.coreproxy.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.Share;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.service.ShareKafkaService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ResultMessageProcessorTest {

    @Spy
    private ObjectMapper objectMapper;
    @Mock
    private ShareKafkaService shareKafkaService;
    @InjectMocks
    private ResultMessageProcessor messageProcessor;

    @Test
    void shouldApplyProcessorIfResultIsTrue() throws JsonProcessingException {
        String message = "{\"error\":null,\"id\":162,\"result\":true}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.POOL, stratumMessage);
        assertTrue(applyResult);
    }

    @Test
    void shouldApplyProcessorIfResultIsNullAndErrorNotNull() throws JsonProcessingException {
        String message = "{\"error\":[20,\"x_x! Internal error\",null],\"id\":1,\"result\":null}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        boolean applyResult = messageProcessor.apply(MessageProcessor.Type.POOL, stratumMessage);
        assertTrue(applyResult);
    }

    @Test
    void shouldSendShareToKafkaWhenResultIsTrue() throws JsonProcessingException {
        ClientContext context = new ClientContext();
        Integer messageId = 77;
        Share share1 = createShare(messageId);
        Share share2 = createShare(88);
        Share share3 = createShare(99);
        context.addShare(share1);
        context.addShare(share2);
        context.addShare(share3);

        String message = "{\"error\":null,\"id\":" + messageId + ",\"result\":true}";
        StratumMessage stratumMessage = objectMapper.readValue(message, StratumMessage.class);

        messageProcessor.process(stratumMessage, context);

        verify(shareKafkaService, times(1)).send(any(Share.class));

        assertEquals(2, context.getShares().size());
        assertTrue(share1.getIsSubmit());
        assertFalse(share2.getIsSubmit());
        assertFalse(share3.getIsSubmit());
    }

    private Share createShare(Integer messageId) {
        return new Share()
                .setMessageId(messageId)
                .setSubmittedTime(LocalDateTime.now())
                .setDifficulty(5L)
                .setIsSubmit(false)
                .setUserId(1L)
                .setMinerId(2L)
                .setPoolId(3L);
    }
}