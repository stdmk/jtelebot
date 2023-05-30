package org.telegram.bot.services.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties
@PropertySource(value = "file:properties.properties", ignoreResourceNotFound = true)
@Data
@Slf4j
public class PropertiesConfig {
    private String telegramBotApiToken;
    private String telegramBotUsername;
    private Long adminId;
    private String openweathermapId;
    private String googleToken;
    private String googleTranslateToken;
    private String screenshotMachineToken;
    private String wolframAlphaToken;
    private String kinopoiskToken;
    private String chatGPTToken;
    private Boolean spyMode;
    private String russianPostLogin;
    private String russianPostPassword;
    @Getter(AccessLevel.NONE)
    private Integer russianPostRequestsLimit;

    public Integer getRussianPostRequestsLimit() {
        if (this.russianPostRequestsLimit == null) {
            this.russianPostRequestsLimit = 100;
            log.error("The parameter russianPostRequestsLimit is not set. Default value set. (100)");
        }

        return this.russianPostRequestsLimit;
    }
}
