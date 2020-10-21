package org.telegram.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.telegram.bot.services.TokenService;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class BotApplication {
    public static void main(String[] args) {
        ApiContextInitializer.init();
        SpringApplication.run(BotApplication.class, args);
    }
}