package top.poools.coreproxy.model;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@ToString(of = {"name", "host", "port"})
@Accessors(chain = true)
public class Pool {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private String hostReserve;
    private Integer portReserve;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private Boolean isDeleted;
    private Integer timeout;
    private String account;
}
