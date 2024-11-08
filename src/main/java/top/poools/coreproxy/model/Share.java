package top.poools.coreproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class Share {
    private Integer messageId;
    private LocalDateTime submittedTime;
    private Long difficulty;
    private Boolean isSubmit;
    private Long userId;
    private Long minerId;
    private Long poolId;
}
