package org.telegram.bot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.starter.TelegramBotInitializer;

@SpringBootTest
public class BotApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private TelegramBotInitializer telegramBotInitializer;

    @Test
    void runTest() {
        Assertions.assertNotNull(applicationContext);
    }

}
