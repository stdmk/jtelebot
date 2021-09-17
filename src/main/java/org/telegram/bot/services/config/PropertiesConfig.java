package org.telegram.bot.services.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties
@PropertySource(value = "file:properties.properties", ignoreResourceNotFound = true)
@Data
public class PropertiesConfig {
    private String telegramBotApiToken;
    private String telegramBotUsername;
    private Long adminId;
    private String openweathermapId;
    private String googleToken;
    private String googleTranslateToken;
    private String screenshotMachineToken;
    private String wolframAlphaToken;
    private Boolean spyMode;
}
