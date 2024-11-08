package top.poools.coreproxy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("core.proxy")
public class CoreProxyProperties {
    private Boolean useReservePoolHost = false;
    private TcpServer tcpServer;
    private Security security;

    @Data
    public static class TcpServer {
        private Integer port;
        private Integer backlog;
        private Connection connection;
    }

    @Data
    public static class Connection {
        private String cleanCron;
        private Long removeAfter;
    }

    @Data
    public static class Security {
        private String basicAuthLogin;
        private String basicAuthPassword;
    }
}
