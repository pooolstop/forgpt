package top.poools.coreproxy.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ConnectionInfo {
    private String id;
    private Long userId;
    private Long minerId;
    private Long poolId;
    private Long difficulty;
    private LocalDateTime lastUpdated;
    private Boolean isAlive;
    private String threadState;
    private String hostAddress;
    private String hostName;
}
