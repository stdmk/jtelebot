package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GetidTest {

    @MockBean
    private UserService userService;

    @MockBean
    private SpeechService speechService;

    @Test
    void getIdByUsernameTest() {
        Update updateWithUserInText;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            updateWithUserInText = objectMapper.readValue(new File("src/test/java/org/telegram/bot/domain/commands/update.json"), Update.class);
        } catch (IOException e) {
            fail("corrupted update.json file");
            return;
        }

        User user = mapUserToEntity(updateWithUserInText.getMessage().getFrom());

        Getid getid = new Getid(userService, speechService);
        String textMessage = getid.getTextMessage(updateWithUserInText);

        Mockito.when(userService.get(textMessage)).thenReturn(user);

        SendMessage sendMessage;
        try {
            sendMessage = getid.parse(updateWithUserInText);
        } catch (BotException e) {
            fail("cannot parse updateWithUserInText");
            return;
        }

        assertEquals("Айди [test](tg://user?id=654321): `654321`\nТвой айди: `654321`", sendMessage.getText());
    }

    @Test
    void getIdByRepliedMessageTest() {
        Update updateWithRepliedMessage;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            updateWithRepliedMessage = objectMapper.readValue(new File("src/test/java/org/telegram/bot/domain/commands/update_with_replied_message.json"), Update.class);
        } catch (IOException e) {
            fail("corrupted update.json file");
            return;
        }

        User user = mapUserToEntity(updateWithRepliedMessage.getMessage().getFrom());
        User otherUser = mapUserToEntity(updateWithRepliedMessage.getMessage().getReplyToMessage().getFrom());

        Getid getid = new Getid(userService, speechService);
        String textMessage = getid.getTextMessage(updateWithRepliedMessage);

        Mockito.when(userService.get(textMessage)).thenReturn(user);
        Mockito.when(userService.get(654320)).thenReturn(otherUser);

        SendMessage sendMessage;
        try {
            sendMessage = getid.parse(updateWithRepliedMessage);
        } catch (BotException e) {
            fail("cannot parse updateWithUserInText");
            return;
        }

        assertEquals("Айди [testtest](tg://user?id=654321): `654321`\n" +
                "Айди testtest2: `654320`\n" +
                "Айди этого чата: `-100123456789`\n" +
                "Твой айди: `654321`", sendMessage.getText());
    }

    private User mapUserToEntity(org.telegram.telegrambots.meta.api.objects.User user) {
        User entity = new User();
        entity.setUserId(user.getId());
        entity.setUsername(user.getUserName());

        return entity;
    }
}