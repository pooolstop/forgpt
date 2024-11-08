package top.poools.coreproxy.exception;

public class ParseMessageException extends RuntimeException {

    public ParseMessageException(String message) {
        super(message);
    }

    public ParseMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseMessageException(Throwable cause) {
        super(cause);
    }
}
