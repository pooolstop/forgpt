package top.poools.coreproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "method", "params", "result", "error"})
public class StratumMessage {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private List<Object> params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private List<Object> error;
}
