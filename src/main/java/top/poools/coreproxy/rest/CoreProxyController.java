package top.poools.coreproxy.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.poools.coreproxy.mapper.ConnectionMapper;
import top.poools.coreproxy.mapper.ShareMapper;
import top.poools.msapi.lk.dto.PoolDto;
import top.poools.coreproxy.mapper.PoolMapper;
import top.poools.coreproxy.server.TcpProxyServer;
import top.poools.msapi.proxy.dto.ConnectionDto;
import top.poools.msapi.proxy.dto.ShareDto;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class CoreProxyController {

    private final TcpProxyServer tcpProxyServer;
    private final PoolMapper poolMapper;
    private final ConnectionMapper connectionMapper;
    private final ShareMapper shareMapper;


    @PutMapping(
            path = "/set-user-pool",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> setUserPool(
            @RequestParam("userId") Long userId,
            @RequestBody PoolDto poolDto
    ) {
        tcpProxyServer.setUserPool(userId, poolMapper.toEntity(poolDto));
        return ResponseEntity.ok("pool has changed for user: " + userId);
    }

    @PutMapping(
            path = "/connection/terminate",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> terminateConnection(
            @RequestParam("minerId") Long minerId
    ) {
        tcpProxyServer.terminateConnection(minerId);
        return ResponseEntity.ok("connection has terminated for miner with id: " + minerId);
    }

    @PutMapping(
            path = "/connection/terminate/by-miners",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> terminateConnectionsByMinerIds(
            @RequestParam("minerIds") List<Long> minerIds
    ) {
        tcpProxyServer.terminateConnections(minerIds);
        return ResponseEntity.ok("connections has terminated for miners: " + minerIds);
    }

    @PutMapping(
            path = "/connection/terminate/all",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> terminateAllConnections() {
        tcpProxyServer.terminateAllConnections();
        return ResponseEntity.ok("all connections has terminated");
    }

    @GetMapping(
            path = "/connections",
            produces = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ConnectionDto>> getConnections() {
        return ResponseEntity.ok(
                tcpProxyServer.getAllConnections().stream()
                        .map(connectionMapper::toDto)
                        .toList());
    }

    @GetMapping(
            path = "/connections/by-user",
            produces = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ConnectionDto>> getConnectionsByUser(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(
                tcpProxyServer.getConnectionsByUser(userId).stream()
                        .map(connectionMapper::toDto)
                        .toList());
    }

    @GetMapping(
            path = "/shares",
            produces = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ShareDto>> getShares() {
        return ResponseEntity.ok(
                tcpProxyServer.getShares().entrySet().stream()
                        .flatMap(entry ->
                                entry.getValue().stream()
                                        .map(e -> shareMapper.toDto(e, entry.getKey())))
                        .toList());
    }

    @GetMapping(
            path = "/shares/by-user",
            produces = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ShareDto>> getSharesByUser(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(
                tcpProxyServer.getSharesByUser(userId).entrySet().stream()
                        .flatMap(entry ->
                                entry.getValue().stream()
                                        .map(e -> shareMapper.toDto(e, entry.getKey())))
                        .toList());
    }
}
