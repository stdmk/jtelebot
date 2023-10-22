package org.telegram.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.bot.config.PropertiesConfig;

import static org.telegram.bot.utils.FileUtils.checkPropertiesFileExists;

@SpringBootApplication
@EnableConfigurationProperties(PropertiesConfig.class)
@EnableScheduling
@EnableAsync()
public class BotApplication {
    public static void main(String[] args) {
        checkPropertiesFileExists();
        SpringApplication.run(BotApplication.class, args);
    }
}