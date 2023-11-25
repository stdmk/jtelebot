package org.telegram.bot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
public class BotApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void runTest() {
        Assertions.assertNotNull(applicationContext);
    }

}
