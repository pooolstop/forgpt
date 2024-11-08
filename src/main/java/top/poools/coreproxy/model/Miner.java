package top.poools.coreproxy.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class Miner {
    private Long id;
    private String name;
    private String info;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private Boolean isDeleted;
    private Long userId;
    private Long poolId;
    private String inetAddress;
    private Integer connectAttempt;
}
