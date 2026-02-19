package org.telegram.bot.config.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram.proxy")
@Data
public class TelegramProxyProperties {

    private boolean enabled;
    private ProxyType type;
    private String host;
    private int port;
    private String username;
    private String password;

    public enum ProxyType {
        HTTP,
        SOCKS
    }

}
