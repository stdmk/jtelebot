package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.bot.services.ShutdownService;

@RequiredArgsConstructor
@Service
public class ShutdownServiceImpl implements ShutdownService {

    private final ConfigurableApplicationContext context;

    public void shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            int exitCode = SpringApplication.exit(context, () -> 0);
            System.exit(exitCode);
        }).start();
    }

}
