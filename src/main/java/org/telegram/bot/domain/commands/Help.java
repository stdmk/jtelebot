package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Help implements CommandParent<SendMessage> {

    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final ChatService chatService;
    private final PropertiesConfig propertiesConfig;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());

        if (textMessage == null || textMessage.length() == 0) {
            log.debug("Request to get general help");
            StringBuilder responseText = new StringBuilder();

            if (checkIsThatAdmin(message.getFrom().getId())) {
                responseText.append("Права администратора успешно предоставлены\n\n");
            }

            Integer accessLevel;
            Integer userAccessLevel = userService.getUserAccessLevel(message.getFrom().getId());
            Integer chatAccessLevel = chatService.getChatAccessLevel(message.getChatId());
            if (userAccessLevel > chatAccessLevel) {
                accessLevel = userAccessLevel;
            } else {
                accessLevel = chatAccessLevel;
            }

            responseText.append("*Без паники!*\n");
            responseText.append("Твой текущий уровень - *").append(accessLevel)
                    .append("* (пользователь - ").append(userAccessLevel)
                    .append("; чат - ").append(chatAccessLevel)
                    .append(")\n");

            List<CommandProperties> commandsList = commandPropertiesService.getAvailableCommandsForLevel(accessLevel);

            responseText.append("Список доступных тебе команд (").append(commandsList.size()).append("):\n");
            commandsList.forEach(commandProperties -> responseText
                    .append("/")
                    .append(commandProperties.getCommandName())
                    .append(" — ")
                    .append(commandProperties.getRussifiedName())
                    .append("\n"));

            responseText.append("Я понимаю команды как на латинице (help), так и на кириллице (помощь)\n")
                        .append("Для получения помощи по определённой команде, напиши 'помощь ИмяКоманды' без кавычек\n");

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableMarkdown(true);
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText(responseText.toString());

            return sendMessage;
        } else {
            log.debug("Request to get help for command: {}", textMessage);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableMarkdown(true);
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText(formatHelpText(commandPropertiesService.getCommand(textMessage).getHelp()));

            return sendMessage;
        }
    }

    /**
     * Formatting raw help data text.
     *
     * @param helpData raw text of help.
     * @return formatted text of help.
     */
    private String formatHelpText(String helpData) {
        StringBuilder preparedText = new StringBuilder();

        int titleEndIndex = helpData.indexOf(",");
        preparedText.append("*Команда:* ").append(helpData, 0, titleEndIndex).append("\n");

        int descEndIndex = helpData.indexOf(",", titleEndIndex + 1);
        preparedText.append("*Описание:* ").append(helpData, titleEndIndex + 1, descEndIndex).append("\n");

        int paramsEndIndex = helpData.indexOf(",", descEndIndex + 1);
        preparedText.append("*Параметры:* ").append(helpData, descEndIndex + 1, paramsEndIndex).append("\n");

        int examplesEndIndex = helpData.indexOf(",", paramsEndIndex + 1);
        preparedText.append("*Примеры:* ").append(helpData, paramsEndIndex + 1, examplesEndIndex).append("\n");

        preparedText.append("*Примечания:* ").append(helpData.substring(examplesEndIndex + 1));

        return preparedText.toString();
    }

    /**
     * Checking if the user is specified by the admin and gives him admin rights.
     *
     * @param userId user id to check.
     */
    private Boolean checkIsThatAdmin(Long userId) {
        Long adminId;
        try {
            adminId = propertiesConfig.getAdminId();
        } catch (Exception e) {
            return false;
        }

        User user = userService.get(userId);
        if (user.getUserId().equals(adminId) && !user.getAccessLevel().equals(AccessLevel.ADMIN.getValue())) {
            user.setAccessLevel(AccessLevel.ADMIN.getValue());
            userService.save(user);
            return true;
        }

        return false;
    }
}
