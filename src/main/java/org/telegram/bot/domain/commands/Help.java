package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
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

    private final Bot bot;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final ChatService chatService;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());

        String responseText;
        if (StringUtils.isEmpty(textMessage)) {
            log.debug("Request to get general help");
            StringBuilder buf = new StringBuilder();

            if (checkIsThatAdmin(message.getFrom().getId())) {
                buf.append("Права администратора успешно предоставлены\n\n");
            }

            Integer accessLevel;
            Integer userAccessLevel = userService.getUserAccessLevel(message.getFrom().getId());
            Integer chatAccessLevel = chatService.getChatAccessLevel(message.getChatId());
            if (userAccessLevel > chatAccessLevel) {
                accessLevel = userAccessLevel;
            } else {
                accessLevel = chatAccessLevel;
            }

            buf.append("<b>Без паники!</b>\n");
            buf.append("Твой текущий уровень - <b>").append(accessLevel)
                    .append("</b> (пользователь - ").append(userAccessLevel)
                    .append("; чат - ").append(chatAccessLevel)
                    .append(")\n");

            List<CommandProperties> commandsList = commandPropertiesService.getAvailableCommandsForLevel(accessLevel);

            buf.append("Список доступных тебе команд (").append(commandsList.size()).append("):\n");
            commandsList.forEach(commandProperties -> buf
                    .append("/")
                    .append(commandProperties.getCommandName())
                    .append(" — ")
                    .append(commandProperties.getRussifiedName())
                    .append(" (").append(commandProperties.getAccessLevel()).append(")")
                    .append("\n"));

            buf.append("Я понимаю команды как на латинице (help), так и на кириллице (помощь)\n")
                        .append("Для получения помощи по определённой команде, напиши 'помощь ИмяКоманды' без кавычек\n");

            responseText = buf.toString();
        } else {
            log.debug("Request to get help for command: {}", textMessage);

            CommandProperties command = commandPropertiesService.getCommand(textMessage);
            if (command == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = formatHelpText(command.getHelp(), command.getAccessLevel());
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Formatting raw help data text.
     *
     * @param help Help entity.
     * @return formatted text of help.
     */
    private String formatHelpText(org.telegram.bot.domain.entities.Help help, Integer level) {
        return "<b>Команда:</b> " + getHelpPartValueWithDefault(help.getName()) + "\n" +
                "<b>Описание:</b> " + getHelpPartValueWithDefault(help.getDescription()) + "\n" +
                "<b>Параметры:</b> " + getHelpPartValueWithDefault(help.getParams()) + "\n" +
                "<b>Примеры:</b> " + getHelpPartValueWithDefault(help.getExamples()) + "\n" +
                "<b>Примечания:</b> " + getHelpPartValueWithDefault(help.getComment()) + "\n" +
                "<b>Уровень:</b> " + level;
    }

    private String getHelpPartValueWithDefault(String value) {
        return value != null ? value : "отсутствуют";
    }

    /**
     * Checking if the user is specified by the admin and gives him admin rights.
     *
     * @param userId user id to check.
     */
    private Boolean checkIsThatAdmin(Long userId) {
        Long adminId = propertiesConfig.getAdminId();

        User user = userService.get(userId);
        if (user.getUserId().equals(adminId) && !user.getAccessLevel().equals(AccessLevel.ADMIN.getValue())) {
            user.setAccessLevel(AccessLevel.ADMIN.getValue());
            userService.save(user);
            return true;
        }

        return false;
    }
}
