package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Help implements Command<SendMessage> {

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
                buf.append("${command.help.grants}\n\n");
            }

            Integer accessLevel;
            Integer userAccessLevel = userService.getUserAccessLevel(message.getFrom().getId());
            Integer chatAccessLevel = chatService.getChatAccessLevel(message.getChatId());
            if (userAccessLevel > chatAccessLevel) {
                accessLevel = userAccessLevel;
            } else {
                accessLevel = chatAccessLevel;
            }

            buf.append("<b>${command.help.dontpanic}!</b>\n");
            buf.append("${command.help.currentlevel} - <b>").append(accessLevel)
                    .append("</b> (${command.help.user} - ").append(userAccessLevel)
                    .append("; ${command.help.chat} - ").append(chatAccessLevel)
                    .append(")\n");

            List<CommandProperties> commandsList = commandPropertiesService.getAvailableCommandsForLevel(accessLevel);

            buf.append("${command.help.listofavailablecommands} (").append(commandsList.size()).append("):\n");
            commandsList.forEach(commandProperties -> buf
                    .append("/")
                    .append(commandProperties.getCommandName())
                    .append(" — ").append("${help.").append(commandProperties.getClassName().toLowerCase()).append(".name}")
                    .append(" (").append(commandProperties.getAccessLevel()).append(")")
                    .append("\n"));

            buf.append("${command.help.specificcommandhelp}\n");

            responseText = buf.toString();
        } else {
            log.debug("Request to get help for command: {}", textMessage);

            CommandProperties command = commandPropertiesService.getCommand(textMessage);
            if (command == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = formatHelpText(command.getClassName().toLowerCase(), command.getAccessLevel());
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String formatHelpText(String commandName, Integer level) {
        return "<b>${command.help.commandinfo.name}:</b> ${help." + commandName + ".name}\n" +
                "<b>${command.help.commandinfo.desc}:</b> ${help." + commandName + ".desc}\n" +
                "<b>${command.help.commandinfo.args}:</b> ${help." + commandName + ".params}\n" +
                "<b>${command.help.commandinfo.examples}:</b> ${help." + commandName + ".examples}\n" +
                "<b>${command.help.commandinfo.comment}:</b> ${help." + commandName + ".comment}\n" +
                "<b>${command.help.commandinfo.level}:</b> " + level;
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
