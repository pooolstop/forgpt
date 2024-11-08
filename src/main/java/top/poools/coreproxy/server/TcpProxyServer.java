package top.poools.coreproxy.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.poools.coreproxy.model.ConnectionInfo;
import top.poools.coreproxy.model.Miner;
import top.poools.coreproxy.model.Pool;
import top.poools.coreproxy.model.Share;
import top.poools.coreproxy.processor.MessageProcessor;
import top.poools.coreproxy.properties.CoreProxyProperties;
import top.poools.coreproxy.service.MinerService;
import top.poools.coreproxy.service.PoolService;
import top.poools.coreproxy.util.InetAddressUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpProxyServer {
    private ServerSocket serverSocket;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final List<MessageProcessor> messageProcessors;
    private final ObjectMapper objectMapper;
    private final CoreProxyProperties properties;
    private final PoolService poolService;
    private final MinerService minerService;
    private static String CURRENT_HOST_ADDRESS;
    private static String CURRENT_HOST_NAME;

    static {
        try {
            CURRENT_HOST_ADDRESS = InetAddress.getLocalHost().getHostAddress();
            CURRENT_HOST_NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("can not get current host address and name: ", e);
        }
    }

    @PostConstruct
    public synchronized void start() {
        log.info("Starting TCP server...");
        try {
            var tcpServer = properties.getTcpServer();
            log.info("TCP Server configuration: port={}, backlog={}", tcpServer.getPort(), tcpServer.getBacklog());
            serverSocket = new ServerSocket(tcpServer.getPort(), tcpServer.getBacklog());
            log.info("Server socket created on port {} with backlog {}", tcpServer.getPort(), tcpServer.getBacklog());
            Thread serverThread = new Thread(() -> {
                try {
                    long connectionNumber = 1;
                    log.info("ServerSocket is Closed:{}. Server is listening for incoming connections...", serverSocket.isClosed());
                    while (!serverSocket.isClosed()) {
                        try {
                            log.info("Waiting new connection {}", serverSocket.toString());
                            Socket clientSocket = serverSocket.accept();
                            var inetAddress = InetAddressUtils.getFullInetAddress(clientSocket);
                            log.info("Accepted new connection from: {}", clientSocket.getInetAddress());
                            var pool = resolvePool(inetAddress);
                            var connection = new Connection(clientSocket, inetAddress, messageProcessors, objectMapper,
                                    pool, connectionNumber++, properties.getUseReservePoolHost());
                            try {
                                connection.start();
                                log.info("Connection with id: {} started. InetAddress: {}", connection.getConnectionId(), inetAddress);
                            } catch (Exception ex) {
                                log.error("Connection {}:{} start failed: ", clientSocket.getInetAddress(), connection.getConnectionId(), ex);
                                throw ex;
                            }
                            connections.put(connection.getConnectionId(), connection);
                            log.debug("Connection with id: {} added to the connection pool", connection.getConnectionId());
                        } catch (Exception ex) {
                            log.error("Error while handling a new connection: ", ex);
                        }
                    }
                } catch (Exception ex) {
                    log.error("Server encountered an error while accepting connections: ", ex);
                }
            });
            serverThread.start();
            log.info("TCP server thread started.");
        } catch (Exception ex) {
            log.error("Failed to start the server: ", ex);
        }
    }

    @PreDestroy
    public synchronized void stop() throws IOException {
        serverSocket.close();
        connections.forEach((key, connection) -> connection.interrupt());
    }

    @Scheduled(cron = "${core.proxy.tcp-server.connection.clean-cron}")
    public void cleanConnections() {
        log.trace("start clean connections");
        int removedConnections = removeDeadConnections();
        log.trace("removed: {} dead connections", removedConnections);
    }

    //todo: add caching
    public List<ConnectionInfo> getAllConnections() {
        return connections.values().stream()
                .map(this::toConnectionInfo)
                .collect(Collectors.toList());
    }

    //todo: add caching
    public List<ConnectionInfo> getConnectionsByUser(Long userId) {
        return connections.values().stream()
                .filter(connection -> userId.equals(connection.getUserId()))
                .map(this::toConnectionInfo)
                .collect(Collectors.toList());
    }

    public Map<String, List<Share>> getShares() {
        return connections.values().stream()
                .collect(Collectors.toMap(Connection::getConnectionId, Connection::getShares));
    }

    public Map<String, List<Share>> getSharesByUser(Long userId) {
        return connections.values().stream()
                .filter(connection -> userId.equals(connection.getUserId()))
                .collect(Collectors.toMap(Connection::getConnectionId, Connection::getShares));
    }

    public void removeConnection(String connectionId) {
        Connection removed = connections.remove(connectionId);
        if (removed != null) {
            removed.interrupt();
            log.info("connection with id: {} and state: {} has removed", connectionId, removed.getState().name());
        }
    }

    public int removeDeadConnections() {
        var currentTime = LocalDateTime.now();
        var removeAfter = properties.getTcpServer().getConnection().getRemoveAfter();
        Set<String> connectionIds = connections.values().stream()
                .filter(conn -> {
                    if (!conn.isAlive()) {
                        log.info("connection is not alive, status: {}", conn.getState().name());
                        return true;
                    }
                    var lastUpdated = conn.getLastUpdated();
                    boolean lastUpdatedExceeded = (lastUpdated != null && ChronoUnit.SECONDS.between(lastUpdated, currentTime) > removeAfter);
                    if (lastUpdatedExceeded) {
                        log.info("connection last updated: {} exceeded {} seconds", lastUpdated, removeAfter);
                        return true;
                    }
                    return false;
                })
                .map(Connection::getConnectionId)
                .collect(Collectors.toSet());
        connectionIds.forEach(this::removeConnection);
        return connectionIds.size();
    }

    public void setUserPool(Long userId, Pool pool) {
        connections.values().stream()
                .filter(connection -> userId.equals(connection.getUserId()))
                .forEach(connection -> connection.setPool(pool));
    }

    public void terminateConnection(Long minerId) {
        connections.values().stream()
                .filter(connection -> minerId.equals(connection.getMinerId()))
                .forEach(connection -> removeConnection(connection.getConnectionId()));
        log.info("connection has terminated for miner [id={}]", minerId);
    }

    public void terminateConnections(List<Long> minerIds) {
        connections.values().stream()
                .filter(connection -> minerIds.contains(connection.getMinerId()))
                .forEach(connection -> removeConnection(connection.getConnectionId()));
        log.info("connections has terminated for miners: {}", minerIds);
    }

    public void terminateAllConnections() {
        connections.values().stream()
                .map(Connection::getConnectionId)
                .forEach(this::removeConnection);
        log.info("all connections has terminated");
    }

    private Pool resolvePool(String inetAddress) {
        List<Miner> miners = minerService.getMinersByInetAddress(inetAddress);
        if (!CollectionUtils.isEmpty(miners)) {
            //sort by miner.updated field
            var poolIds = miners.stream()
                    .sorted(Comparator.comparing(Miner::getUpdateDate).reversed())
                    .map(Miner::getPoolId)
                    .distinct()
                    .toList();
            if (poolIds.isEmpty()) {
                log.info("pool is not specified. Default pool will be used");
                return poolService.getDefaultPool();
            } else if (poolIds.size() == 1) {
                log.info("inetAddress: {} belongs to one pool: {}", inetAddress, poolIds.get(0));
                return poolService.getPoolById(poolIds.get(0));
            } else {
                log.info("inetAddress: {} belongs to several pools: {}, miners: {}. Pool with latest miner.updateTs will be used",
                        inetAddress, poolIds, miners.stream().map(Miner::getId).toList());
                return poolService.getPoolById(poolIds.get(0));
            }
        } else {
            log.info("miners with inetAddress {} not found. Default pool will be used", inetAddress);
            return poolService.getDefaultPool();
        }
    }

    private ConnectionInfo toConnectionInfo(Connection connection) {
        return new ConnectionInfo()
                .setId(connection.getConnectionId())
                .setUserId(connection.getUserId())
                .setMinerId(connection.getMinerId())
                .setPoolId(connection.getPoolId())
                .setDifficulty(connection.getDifficulty())
                .setLastUpdated(connection.getLastUpdated())
                .setIsAlive(connection.isAlive())
                .setThreadState(connection.getState().name())
                .setHostAddress(CURRENT_HOST_ADDRESS)
                .setHostName(CURRENT_HOST_NAME);
    }
}