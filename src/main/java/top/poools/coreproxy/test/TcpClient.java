package top.poools.coreproxy.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.poools.coreproxy.model.StratumMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
public class TcpClient  extends Thread {

    private final String host;
    private final int port;
    private final String clientName;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public TcpClient(String host, int port, String clientName) {
        super();
        this.host = host;
        this.port = port;
        this.clientName = clientName;
    }

    @Override
    public void run() {
        try {
            Socket clientSocket = new Socket(host, port);
            //clientSocket.setSoTimeout(30000);
            //clientSocket.setKeepAlive(true);
            try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                int count = 100;
                int current = 0;
                while (current <= count) {
                    var subscribeRequest = createMessage(++current, "mining.subscribe",
                            List.of("MyMiner/" + clientName, "null", host, port));
                    sendMessage(writer, subscribeRequest);
                    log.debug("{}: subscribe request: {}", clientName, subscribeRequest);
                    var subscribeResponse = readMessage(reader, StratumMessage.class);
                    log.debug("{}: subscribe response: {}", clientName, subscribeResponse);

                    var authorizeRequest = createMessage(++current, "mining.authorize",
                            List.of(clientName + ".miner_" + clientName, "worker_password"));
                    sendMessage(writer, authorizeRequest);
                    log.debug("{}: authorize request: {}", clientName, authorizeRequest);
                    var authorizeResponse = readMessage(reader, StratumMessage.class);
                    log.debug("{}: authorize response: {}", clientName, authorizeResponse);

                    var setTargetRequest = readMessage(reader, StratumMessage.class);
                    log.debug("{}: set_target request: {}", clientName, setTargetRequest);

                    var notifyRequest = readMessage(reader, StratumMessage.class);
                    log.debug("{}: notify request: {}", clientName, notifyRequest);

                    var submitRequest = createMessage(++current, "mining.submit",
                            List.of(clientName + ".miner_" + clientName,
                                    UUID.randomUUID().toString(), UUID.randomUUID().toString(), "solution"));
                    sendMessage(writer, submitRequest);
                    log.debug("{}: submit request: {}", clientName, submitRequest);
                    var submitResponse = readMessage(reader, StratumMessage.class);
                    log.debug("{}: submit response: {}", clientName, submitResponse);

                    sleep();
                }
            } catch (Exception ex) {
                log.error("error occur: ", ex);
            }

            clientSocket.close();
            log.info("{}: client socket CLOSED", clientName);
        } catch (IOException e) {
            log.error("client socket error: ", e);
        }
    }

    private void sendMessage(
            PrintWriter writer,
            StratumMessage stratumMessage
    ) throws IOException {
        writer.println(objectMapper.writeValueAsString(stratumMessage));
        writer.flush();
    }

    private <T> T readMessage(BufferedReader reader, Class<T> clazz) throws IOException {
        String message;
        T result = null;
        while ((message = reader.readLine()) != null) {
            result = objectMapper.readValue(message, clazz);
            break;
        }
        return result;
    }

    private StratumMessage createMessage(Integer id, String method, List<Object> params) {
        return new StratumMessage()
                .setId(id)
                .setMethod(method)
                .setParams(params);
    }

    private void sleep() {
        try {
            Thread.sleep(random.nextLong(1000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
