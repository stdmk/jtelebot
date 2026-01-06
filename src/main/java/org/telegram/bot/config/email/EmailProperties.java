package org.telegram.bot.config.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
@Getter
@Setter
public class EmailProperties {
    private ImapsConfig imaps;
    private SmtpConfig smtp;

    @Getter
    @Setter
    public static class ImapsConfig {
        private String user;
        private String password;
        private String host;
        private Integer port;
        private SslConfig ssl;
        private Boolean peek;
    }

    @Getter
    @Setter
    public static class SmtpConfig {
        private String user;
        private String password;
        private String host;
        private Integer port;
        private Boolean auth;
        private SslConfig ssl;
        private Long connectiontimeout;
        private Long timeout;
        private Long writetimeout;
    }

    @Getter
    @Setter
    public static class SslConfig {
        private Boolean enable;
    }

}
