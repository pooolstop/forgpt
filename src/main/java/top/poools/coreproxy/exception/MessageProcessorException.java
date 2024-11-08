package top.poools.coreproxy.exception;

public class MessageProcessorException extends BusinessException {

    public MessageProcessorException(String message) {
        super(message, 10);
    }

    public MessageProcessorException(String message, Throwable cause) {
        super(message, cause, 10);
    }

    public MessageProcessorException(Throwable cause) {
        super(cause, 10);
    }
}
