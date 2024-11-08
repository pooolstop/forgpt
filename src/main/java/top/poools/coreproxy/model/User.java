package top.poools.coreproxy.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Accessors(chain = true)
public class User {
    private Long id;
    private String login;
    private String password;
    private String name;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private Boolean isDeleted;
    private String email;
    private String phone;
    private Boolean blocked;
    private Set<Miner> miners;
}