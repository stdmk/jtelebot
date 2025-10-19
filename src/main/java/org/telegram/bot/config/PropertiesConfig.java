package org.telegram.bot.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

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
    private String screenshotMachineDevice;
    private String screenshotMachineDimension;
    private String screenshotMachineFormat;
    private String screenshotMachineTimeoutMs;
    private String wolframAlphaToken;
    private String kinopoiskToken;
    private String chatGPTToken;
    private Boolean spyMode;
    private String russianPostLogin;
    private String russianPostPassword;
    @Getter(AccessLevel.NONE)
    private Integer russianPostRequestsLimit;
    private Integer chatGPTContextSize = 16;
    private Integer chatGPTTokensSize = 0;
    private List<String> chatGPTModelsAvailable;
    private String defaultLanguage = "en";
    private String xmlTvFileUrl;
    private String saluteSpeechSecret;
    private String gigaChatSecret;
    private String virusTotalApiKey;
    private String ftpBackupUrl;
    private Integer daysBeforeExpirationBackup;
    private Long maxBackupsSizeBytes;

    public Integer getRussianPostRequestsLimit() {
        if (this.russianPostRequestsLimit == null) {
            this.russianPostRequestsLimit = 100;
            log.error("The parameter russianPostRequestsLimit is not set. Default value set. (100)");
        }

        return this.russianPostRequestsLimit;
    }
}
