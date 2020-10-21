package org.telegram.bot.domain.commands;

import liquibase.util.csv.CSVReader;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@AllArgsConstructor
public class Help extends CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Help.class);

    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final ChatService chatService;

    @Override
    public SendMessage parse(Update update) {
        String textMessage = TextUtils.cutCommandInText(update.getMessage().getText());

        if (textMessage == null || textMessage.length() == 0) {
            log.debug("Requst to get general help");
            StringBuilder responseText = new StringBuilder();
            responseText.append("*Без паники!*\n");

            Integer accessLevel;
            Integer userAccessLevel = userService.getUserAccessLevel(update.getMessage().getFrom().getId());
            Integer chatAccessLevel = chatService.getChatAccessLevel(update.getMessage().getChatId());
            if (userAccessLevel > chatAccessLevel) {
                accessLevel = userAccessLevel;
            } else {
                accessLevel = chatAccessLevel;
            }

            responseText.append("Твой текущий уровень - *").append(accessLevel)
                    .append("* (пользователь - ").append(userAccessLevel)
                    .append("; чат - ").append(chatAccessLevel)
                    .append(")\n");

            List<CommandProperties> commandsList = commandPropertiesService.getAvailableCommandsForLevel(accessLevel);

            responseText.append("Список доступных тебе команд:\n");
            commandsList.forEach(commandProperties -> responseText
                    .append("/")
                    .append(commandProperties.getCommandName())
                    .append(" — ")
                    .append(commandProperties.getRussifiedName())
                    .append("\n"));

            responseText.append("Я понимаю команды как на латинице (help), так и на кириллице (помощь)\n")
                        .append("Для получения помощи по определённой команде, напиши 'помощь ИмяКоманды' без кавычек\n");

            return new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setParseMode(ParseModes.MARKDOWN.getValue())
                    .setText(responseText.toString());
        } else {
            log.debug("Request to get help for command: {}", textMessage);
            return new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setParseMode(ParseModes.MARKDOWN.getValue())
                    .setText(prepareHelpText(commandPropertiesService.findCommandByName(textMessage).getHelp()));
        }
    }

    private String prepareHelpText(String helpData) {
        StringBuilder preparedText = new StringBuilder();

        int titleEndIndex = helpData.indexOf(",");
        preparedText.append("*Команда:* ").append(helpData, 0, titleEndIndex).append("\n");

        int descEndIndex = helpData.indexOf(",", titleEndIndex + 1);
        preparedText.append("*Описание:* ").append(helpData, titleEndIndex + 1, descEndIndex).append("\n");

        int paramsEndIndex = helpData.indexOf(",", descEndIndex + 1);
        preparedText.append("*Параметры:* ").append(helpData, descEndIndex + 1, paramsEndIndex).append("\n");

        int examplesEndIndex = helpData.indexOf(",", paramsEndIndex + 1);
        preparedText.append("Примеры*:* ").append(helpData, paramsEndIndex + 1, examplesEndIndex).append("\n");

        preparedText.append("Примечания*:* ").append(helpData.substring(examplesEndIndex + 1));

        return preparedText.toString();
    }
}
