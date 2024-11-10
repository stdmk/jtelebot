package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DisableCommand;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.TextUtils;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class Help implements Command {

    private final Bot bot;
    private final CommandPropertiesService commandPropertiesService;
    private final DisableCommandService disableCommandService;
    private final UserService userService;
    private final ChatService chatService;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        bot.sendTyping(message.getChatId());

        String commandArgument = message.getCommandArgument();
        String responseText;
        if (TextUtils.isEmpty(commandArgument)) {
            log.debug("Request to get general help");
            StringBuilder buf = new StringBuilder();

            if (checkIsThatAdmin(user.getUserId())) {
                buf.append("${command.help.grants}\n\n");
            }

            Integer accessLevel;
            Integer userAccessLevel = userService.getUserAccessLevel(user.getUserId());
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

            List<Long> disabledCommandsForThisChatIds = disableCommandService.getByChat(chat)
                    .stream()
                    .map(DisableCommand::getCommandProperties)
                    .map(CommandProperties::getId)
                    .toList();
            List<CommandProperties> commandsList = commandPropertiesService.getAvailableCommandsForLevel(accessLevel)
                    .stream()
                    .filter(commandProperties -> !disabledCommandsForThisChatIds.contains(commandProperties.getId()))
                    .toList();

            if (!disabledCommandsForThisChatIds.isEmpty()) {
                buf.append("${command.help.countofdisabled}: ").append(disabledCommandsForThisChatIds.size()).append("\n");
            }

            buf.append("${command.help.listofavailablecommands} (").append(commandsList.size()).append("):\n");
            commandsList.forEach(commandProperties -> buf
                    .append("/")
                    .append(commandProperties.getCommandName())
                    .append(" — ").append("${help.").append(commandProperties.getClassName().toLowerCase(Locale.ROOT)).append(".name}")
                    .append(" (").append(commandProperties.getAccessLevel()).append(")")
                    .append("\n"));

            buf.append("${command.help.specificcommandhelp}\n");

            responseText = buf.toString();
        } else {
            log.debug("Request to get help for command: {}", commandArgument);

            CommandProperties command = commandPropertiesService.getCommand(commandArgument);
            if (command == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = formatHelpText(command.getClassName().toLowerCase(Locale.ROOT), command.getAccessLevel());
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setWebPagePreview(false)
                        .setFormattingStyle(FormattingStyle.HTML)));
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
    private boolean checkIsThatAdmin(Long userId) {
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
