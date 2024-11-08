package top.poools.coreproxy.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import top.poools.coreproxy.exception.BusinessException;
import top.poools.coreproxy.exception.ConnectionException;
import top.poools.coreproxy.exception.FatalProcessorException;
import top.poools.coreproxy.exception.ParseMessageException;
import top.poools.coreproxy.model.Pool;
import top.poools.coreproxy.model.ClientContext;
import top.poools.coreproxy.model.Share;
import top.poools.coreproxy.model.StratumMessage;
import top.poools.coreproxy.model.SyncWrapper;
import top.poools.coreproxy.processor.MessageProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static top.poools.coreproxy.log.MdcFields.CLIENT_IP_FIELD;
import static top.poools.coreproxy.log.MdcFields.CONNECTION_ID_FIELD;
import static top.poools.coreproxy.log.MdcFields.MINER_FIELD;
import static top.poools.coreproxy.log.MdcFields.POOL_FIELD;
import static top.poools.coreproxy.log.MdcFields.USER_FIELD;
import static top.poools.coreproxy.processor.MessageProcessor.SKIP_RESULT;

@Slf4j
@EqualsAndHashCode(of = "connectionId", callSuper = false)
public class Connection extends Thread {
    private final SyncWrapper<Socket> clientSocket = new SyncWrapper<>();
    private final List<MessageProcessor> messageProcessors;
    private final ObjectMapper objectMapper;
    private final SyncWrapper<PrintWriter> clientWriter = new SyncWrapper<>();
    private final SyncWrapper<BufferedReader> clientReader = new SyncWrapper<>();
    private final SyncWrapper<PrintWriter> poolWriter = new SyncWrapper<>();
    private final SyncWrapper<BufferedReader> poolReader = new SyncWrapper<>();
    private final SyncWrapper<Socket> poolSocket = new SyncWrapper<>();
    private final SyncWrapper<Thread> poolConnection = new SyncWrapper<>();
    private final ClientContext context = new ClientContext();
    private final Lock closePoolLock = new ReentrantLock();
    private final Long number;
    private final String clientIp;
    private final Boolean useReservePoolHost;

    @Getter
    private final String connectionId = UUID.randomUUID().toString();

    public Connection(
            Socket socket,
            String inetAddress,
            List<MessageProcessor> messageProcessors,
            ObjectMapper objectMapper,
            Pool pool,
            Long number,
            Boolean useReservePoolHost
    ) {
        super("client-thread-" + number);
        try {
            MDC.put(CONNECTION_ID_FIELD, connectionId);
            this.number = number;
            this.useReservePoolHost = useReservePoolHost;
            this.clientSocket.set(socket);
            this.clientIp = socket.getInetAddress().toString();
            this.messageProcessors = messageProcessors;
            this.objectMapper = objectMapper;
            this.context.setPool(pool);
            this.context.setInetAddress(inetAddress);
            this.clientWriter.set(new PrintWriter(clientSocket.get().getOutputStream(), true));
            this.clientReader.set(new BufferedReader(new InputStreamReader(clientSocket.get().getInputStream())));
        } catch (Exception ex) {
            log.error("create connection error: ", ex);
            throw new ConnectionException("Create connection error", ex);
        } finally {
            MDC.remove(CONNECTION_ID_FIELD);
        }
    }

    public Long getUserId() {
        var userContext = context.getUser();
        return userContext != null ? userContext.getId() : null;
    }

    public Long getMinerId() {
        var minerContext = context.getMiner();
        return minerContext != null ? minerContext.getId() : null;
    }

    public Long getPoolId() {
        var poolContext = context.getPool();
        return poolContext != null ? poolContext.getId() : null;
    }

    public LocalDateTime getLastUpdated() {
        return context.getLastUpdated();
    }

    public Long getDifficulty() {
        return context.getDifficulty();
    }

    public List<Share> getShares() {
        return new ArrayList<>(context.getShares().values());
    }

    public void setPool(Pool pool) {
        //todo: need implement and synchronized
        /*try {
            MDC.put(CONNECTION_ID_FIELD, connectionId);
            checkConnection(pool);
            if (poolLock.tryLock(5, TimeUnit.SECONDS)) {
                var prevPool = context.getPool();
                context.setPool(pool);
                //poolConnection.get().interrupt();
                closePoolConnection();
                createPoolConnection();
                log.info("pool has changed from {} to {}", prevPool, pool);
            }
        } catch (Exception ex) {
            log.error("set pool error: ", ex);
            throw new ConnectionException("Set pool error", ex);
        } finally {
            poolLock.unlock();
            MDC.remove(CONNECTION_ID_FIELD);
        }*/
    }

    @Override
    public void interrupt() {
        closeAll();
        super.interrupt();
    }

    @Override
    public void run() {
        MDC.put(CONNECTION_ID_FIELD, connectionId);
        //create pool connection
        createPoolConnection();
        try {
            String inputLine;
            while ((inputLine = clientReader.get().readLine()) != null) {
                putFieldsToMDC();
                log.info("message from client: {}", inputLine);
                try {
                    String result = processMessage(inputLine, MessageProcessor.Type.CLIENT);
                    if (!SKIP_RESULT.equals(result)) {
                        poolWriter.get().println(result);
                    }
                } catch (FatalProcessorException ex) {
                    throw ex;
                } catch (Exception e) {
                    log.error("process message error: ", e);
                    var errorMessage = createErrorMessage(e);
                    clientWriter.get().println(errorMessage);
                }
            }
        } catch (Exception ex) {
            //
            log.error("client thread error: ", ex);
        } finally {
            //close all connections
            closeAll();
            log.info("context share size: {}", context.getShares().size());
        }
        log.info(getName() + " terminated");
        MDC.clear();
    }

    public void closeAll() {
        try {
            if (closePoolLock.tryLock(5, TimeUnit.SECONDS)) {
                closePoolConnection();
                closeClientConnection();
            }
        } catch (InterruptedException ex) {
            log.error("close thread interrupted: ", ex);
        } finally {
            closePoolLock.unlock();
        }
    }

    private void createPoolConnection() {
        try {
            var pool = context.getPool();
            var host = this.useReservePoolHost ? pool.getHostReserve() : pool.getHost();
            var socket = new Socket(host, pool.getPort());
            socket.setSoTimeout(getTimeoutOrDefault(pool, 0));
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            poolSocket.set(socket);
            poolWriter.set(new PrintWriter(poolSocket.get().getOutputStream(), true));
            poolReader.set(new BufferedReader(new InputStreamReader(poolSocket.get().getInputStream())));
            var poolThreadName = "pool-thread-" + this.number;
            var poolThread = createPoolThread(poolThreadName);
            log.debug("pool connection: {} created", poolThreadName);
            poolThread.start();
            log.info("pool connection: {} started", poolThreadName);
            poolConnection.set(poolThread);
        } catch (Exception ex) {
            log.error("pool: {} thread create/start failed: ", context.getPool(), ex);
            throw new ConnectionException("Create/start pool error", ex);
        }
    }

    private Thread createPoolThread(String threadName) {
        return new Thread(() -> {
            try {
                String inputLine;
                while ((inputLine = poolReader.get().readLine()) != null) {
                    putFieldsToMDC();
                    log.info("message from pool: {}", inputLine);
                    try {
                        String result = processMessage(inputLine, MessageProcessor.Type.POOL);
                        if (!SKIP_RESULT.equals(result)) {
                            clientWriter.get().println(result);
                        }
                    } catch (FatalProcessorException ex) {
                        throw ex;
                    } catch (Exception e) {
                        log.error("process message error: ", e);
                        var errorMessage = createErrorMessage(e);
                        clientWriter.get().println(errorMessage);
                    }
                }
            } catch (Exception ex) {
                //
                log.error("pool thread error: ", ex);
            } finally {
                //close all connections
                closeAll();
                log.info("context share size: {}", context.getShares().size());
                MDC.clear();
            }
            log.debug(threadName + " terminated");
        }, threadName);
    }

    private String processMessage(String input, MessageProcessor.Type processorType) {
        StratumMessage stratumMessage = parsePayload(input);
        return messageProcessors.stream()
                .filter(p -> p.apply(processorType, stratumMessage))
                .findAny()
                .map(p -> p.process(stratumMessage, context))
                .orElse(input);
    }

    private String createErrorMessage(Exception exception) {
        Integer errorCode;
        String errorMessage;
        if (exception instanceof BusinessException) {
            errorCode = ((BusinessException) exception).getCode();
            errorMessage = exception.getMessage();
        } else {
            errorCode = null;
            errorMessage = "technical error";
        }
        var stratumMessage = new StratumMessage()
                .setResult(Boolean.FALSE)
                .setError(Arrays.asList(errorCode, errorMessage, null));
        try {
            return objectMapper.writeValueAsString(stratumMessage);
        } catch (Exception ex) {
            throw new ParseMessageException("Stratum message write error", ex);
        }
    }

    private StratumMessage parsePayload(String message) {
        try {
            return objectMapper.readValue(message, StratumMessage.class);
        } catch (Exception e) {
            throw new ParseMessageException("Stratum message (" + message + ") parse error", e);
        }
    }

    private void checkConnection(Pool pool) {
        //todo: check pool connection, ping, e t c
    }

    private boolean checkPoolConnection() {
        return poolConnection.get() != null && poolConnection.get().isAlive();
    }

    private int getTimeoutOrDefault(Pool pool, int defaultTimeout) {
        return pool.getTimeout() != null ? pool.getTimeout() : defaultTimeout;
    }

    private void closePoolConnection() {
        try {
            if (poolSocket.get() != null && !poolSocket.get().isClosed()) {
                if (poolWriter.get() != null) {
                    poolWriter.get().close();
                }
                if (poolReader.get() != null) {
                    poolReader.get().close();
                }
                poolSocket.get().close();
                log.info("pool socket closed");
            }
        } catch (Exception ex) {
            log.error("close pool socket error: ", ex);
        }
    }

    private void closeClientConnection() {
        try {
            if (clientSocket.get() != null && !clientSocket.get().isClosed()) {
                if (clientWriter.get() != null) {
                    clientWriter.get().close();
                }
                if (clientReader.get() != null) {
                    clientReader.get().close();
                }
                clientSocket.get().close();
                log.info("client socket closed");
            }
        } catch (Exception ex) {
            log.error("close client socket error: ", ex);
        }
    }

    private void putFieldsToMDC() {
        MDC.put(CONNECTION_ID_FIELD, connectionId);
        MDC.put(CLIENT_IP_FIELD, clientIp);
        var user = context.getUser();
        if (user != null) {
            MDC.put(USER_FIELD, user.getLogin());
        }
        var miner = context.getMiner();
        if (miner != null) {
            MDC.put(MINER_FIELD, miner.getName());
        }
        var pool = context.getPool();
        if (pool != null) {
            MDC.put(POOL_FIELD, pool.getName());
        }
    }
}