package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.pro.packaged.C;
import liquibase.pro.packaged.U;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.io.IOException;

@TestConfiguration
public class TestConfig {

    private static final String UPDATE_PATH = "src/test/java/org/telegram/bot/domain/commands/update.json";
    private static final String UPDATE_WITH_REPLIED_MESSAGE_PATH = "src/test/java/org/telegram/bot/domain/commands/update_with_replied_message.json";

    @Bean(name = "update")
    public static Update getCommonUpdate() {
        return readUpdateFromFile(UPDATE_PATH);
    }

    @Bean(name = "repliedUpdate")
    public Update getUpdateWithRepliedMessage() {
        return readUpdateFromFile(UPDATE_WITH_REPLIED_MESSAGE_PATH);
    }

    @Bean(name = "emptyTextMessageUpdate")
    public Update getUpdateWithEmptyTextMessage() {
        Update update = readUpdateFromFile(UPDATE_PATH);
        assert update != null;
        update.getMessage().setText(null);

        return update;
    }

    @Bean(name = "user")
    public User getUser() {
        return mapUserToEntity(getCommonUpdate().getMessage().getFrom());
    }

    @Bean(name = "otherUser")
    public User getOtherUser() {
        return mapUserToEntity(getUpdateWithRepliedMessage().getMessage().getFrom());
    }

    @Bean(name = "chat")
    public Chat getChat() {
        return mapChatToEntity(getUpdateWithRepliedMessage().getMessage().getChat());
    }

    private static User mapUserToEntity(org.telegram.telegrambots.meta.api.objects.User user) {
        User entity = new User();
        entity.setUserId(user.getId());
        entity.setUsername(user.getUserName());

        return entity;
    }

    private static Chat mapChatToEntity(org.telegram.telegrambots.meta.api.objects.Chat chat) {
        Chat entity = new Chat();
        entity.setChatId(chat.getId());

        return entity;
    }

    private static Update readUpdateFromFile(String path) {
        Update update;
        File f = new File(path);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            update = objectMapper.readValue(f, Update.class);
        } catch (IOException e) {
            return null;
        }

        return update;
    }
}
