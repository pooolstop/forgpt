package top.poools.coreproxy.util;

import io.micrometer.common.util.StringUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketOption;
import java.util.Comparator;

@Slf4j
@UtilityClass
public class InetAddressUtils {

    public static String getFullInetAddress(Socket socket) {
        var inetAddress = socket.getInetAddress();
        String extraParams = getExtraParams(socket);
        String hostAddress = inetAddress.getHostAddress();
        if (StringUtils.isNotBlank(extraParams)) {
            return hostAddress + "/" + extraParams;
        }
        return hostAddress;
    }

    private static String getExtraParams(Socket socket) {
        try {
            StringBuilder builder = new StringBuilder();
            var sorted = socket.supportedOptions().stream()
                    .sorted(Comparator.comparing(SocketOption::name))
                    .toList();
             for (SocketOption<?> socketOption : sorted) {
                 var option = socket.getOption(socketOption);
                 if (option != null) {
                     builder.append(socketOption.name()).append(":").append(option).append(";");
                 }
             }
             return builder.toString();
        } catch (IOException e) {
            log.error("can not get extra params from socket");
        }
        return null;
    }
}
