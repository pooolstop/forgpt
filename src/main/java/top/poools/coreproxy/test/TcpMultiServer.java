package top.poools.coreproxy.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.poools.coreproxy.model.StratumMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class TcpMultiServer {
    private final List<ServerSocket> serverSocketList = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void start() throws IOException {
        //runServerSocket(7777);
        //runServerSocket(9999);
    }

    private void runServerSocket(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port, 10_000);
        var serverThread = new Thread(() -> {
            while (true) {
                try {
                    new TcpConnection(serverSocket.accept(), port).start();
                } catch (Exception ex) {
                    log.error("server {} error: ", port, ex);
                }
            }
        });
        serverThread.start();
        serverSocketList.add(serverSocket);
    }

    @PreDestroy
    public void stop() {
        serverSocketList.forEach(s -> {
            try {
                s.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static class TcpConnection extends Thread {
        private final Socket clientSocket;
        private final int serverPort;
        private final ObjectMapper objectMapper = new ObjectMapper();
        Random random = new Random();

        public TcpConnection(Socket socket, int serverPort) {
            this.clientSocket = socket;
            this.serverPort = serverPort;
        }

        public void run() {
            try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String inputLine;
                while ((inputLine = reader.readLine()) != null) {

                    sleep(random.nextLong(10) + 1);
                    var message = readMessage(inputLine);
                    log.debug("server {} received message: {}", serverPort, message);

                    StratumMessage result;
                    if ("mining.subscribe".equals(message.getMethod())) {
                        result = new StratumMessage()
                                .setId(message.getId())
                                .setResult(Arrays.asList(
                                        Arrays.asList(
                                                Arrays.asList("mining.notify", "0000901b1"),
                                                Arrays.asList("mining.set_difficulty", "0000901b2")
                                        ), "0000901b", 8)
                                )
                                .setError(null);
                    } else {
                        result = new StratumMessage()
                                .setId(message.getId())
                                .setResult(Boolean.TRUE)
                                .setError(null);
                    }
                    sendMessage(writer, result);
                    log.debug("server {} sent message: {}", serverPort, result);
                    if ("mining.authorize".equals(message.getMethod())) {
                        sleep(random.nextLong(500) + 1);
                        var setTargetMessage = new StratumMessage()
                                .setMethod("mining.set_target")
                                .setParams(List.of("13435568634362542"));
                        sendMessage(writer, setTargetMessage);
                        log.debug("server {} sent message: {}", serverPort, setTargetMessage);

                        sleep(random.nextLong(500) + 1);
                        var notifyMessage = new StratumMessage()
                                .setMethod("mining.notify")
                                .setParams(List.of("d70fd222", 12324, "234554", true));
                        sendMessage(writer, notifyMessage);
                        log.debug("server {} sent message: {}", serverPort, notifyMessage);
                    }
                }
            } catch (Exception e) {
                //
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private StratumMessage readMessage(String input) throws IOException {
            return objectMapper.readValue(input, StratumMessage.class);
        }

        private void sendMessage(
                PrintWriter writer,
                StratumMessage stratumMessage
        ) throws IOException {
            writer.println(objectMapper.writeValueAsString(stratumMessage));
            writer.flush();
        }
    }
}
