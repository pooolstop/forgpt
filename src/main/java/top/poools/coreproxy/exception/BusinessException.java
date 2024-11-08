package top.poools.coreproxy.exception;

import lombok.Getter;

public class BusinessException extends RuntimeException {

    @Getter
    private final Integer code;

    public BusinessException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause, Integer code) {
        super(message, cause);
        this.code = code;
    }

    public BusinessException(Throwable cause, Integer code) {
        super(cause);
        this.code = code;
    }
}
