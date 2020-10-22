package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.services.PropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Getid extends CommandParent<SendMessage> {

    private final PropertiesService propertiesService;
    private final UserService userService;

    @Override
    public SendMessage parse(Update update) {
        StringBuilder responseText = new StringBuilder();

        Long chatId = update.getMessage().getChatId();
        Integer userId = update.getMessage().getFrom().getId();
        int adminId;
        try {
            adminId = Integer.parseInt(propertiesService.get("adminId"));
        } catch (Exception e) {
            adminId = 0;
        }

        User user = userService.get(userId);
        if (user.getUserId().equals(adminId) && !user.getAccessLevel().equals(AccessLevels.ADMIN.getValue())) {
            user.setAccessLevel(AccessLevels.ADMIN.getValue());
            userService.save(user);
            responseText.append("Права администратора успешно назначены\n");
        }

        if (chatId < 0) {
            responseText.append("Айди этого чата: `").append(chatId).append("`\n");
        }

        responseText.append("Твой айди: `").append(userId).append("`");

        return new SendMessage()
                .setChatId(chatId)
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText(responseText.toString());
    }
}
