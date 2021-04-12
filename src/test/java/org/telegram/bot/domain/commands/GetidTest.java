package org.telegram.bot.domain.commands;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestConfig.class})
class GetidTest {

    @MockBean
    private UserService userService;
    @MockBean
    private SpeechService speechService;

    @Autowired
    Update update;
    @Autowired
    Update repliedUpdate;
    @Autowired
    User user;
    @Autowired
    User otherUser;

    @Test
    void getIdByUsernameTest() {
        Getid getid = new Getid(userService, speechService);
        String textMessage = getid.getTextMessage(update);

        Mockito.when(userService.get(textMessage)).thenReturn(user);

        SendMessage sendMessage;
        try {
            sendMessage = getid.parse(update);
        } catch (BotException e) {
            fail("cannot parse updateWithUserInText");
            return;
        }

        assertEquals("Айди [test](tg://user?id=654321): `654321`\nТвой айди: `654321`", sendMessage.getText());
    }

    @Test
    void getIdByRepliedMessageTest() {
        Getid getid = new Getid(userService, speechService);
        String textMessage = getid.getTextMessage(repliedUpdate);

        Mockito.when(userService.get(textMessage)).thenReturn(user);
        Mockito.when(userService.get(654320)).thenReturn(otherUser);

        SendMessage sendMessage;
        try {
            sendMessage = getid.parse(repliedUpdate);
        } catch (BotException e) {
            fail("cannot parse updateWithUserInText");
            return;
        }

        assertEquals("Айди [test](tg://user?id=654321): `654321`\n" +
                "Айди testtest: `654321`\n" +
                "Айди этого чата: `-100123456789`\n" +
                "Твой айди: `654321`", sendMessage.getText());
    }
}