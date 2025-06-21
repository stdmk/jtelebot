package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.utils.TextUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Level implements Command {

    private final Bot bot;
    private final UserService userService;
    private final ChatService chatService;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        String responseText;

        if (commandArgument == null) {
            responseText = getLevelOfChat(message.getChatId());
        } else {
            int i = commandArgument.indexOf(" ");
            if (i < 0) {
                try {
                    changeChatLevel(message.getChatId(), Integer.parseInt(commandArgument));
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
                } catch (NumberFormatException e) {
                    User user = userService.get(commandArgument);
                    if (user == null) {
                        CommandProperties commandProperties = commandPropertiesService.getCommand(commandArgument);
                        if (commandProperties == null) {
                            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                        }
                        responseText = getLevelOfCommand(commandProperties);
                    } else {
                        responseText = getLevelOfUser(user);
                    }
                }
            } else {
                String argument = commandArgument.substring(0, i);
                int level;
                try {
                    level = Integer.parseInt(commandArgument.substring(i + 1));
                } catch (NumberFormatException e) {
                    log.debug("Cannot parse level in {}", commandArgument);
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                User user = userService.get(argument);
                if (user == null) {
                    CommandProperties commandProperties = commandPropertiesService.getCommand(argument);
                    if (commandProperties == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                    changeCommandLevel(commandProperties, level);
                } else {
                    changeUserLevel(user, level);
                }

                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private String getLevelOfChat(Long chatId) {
        log.debug("Request to get level of chat with id {}", chatId);

        if (chatId > 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return "${command.level.grouplevel} - " + chatService.getChatAccessLevel(chatId);
    }

    private void changeChatLevel(Long chatId, int level) {
        org.telegram.bot.domain.entities.Chat chatToUpdate = chatService.get(chatId);

        log.debug("Request to change level of chat {} from {} to {}", chatToUpdate.getChatId(), chatToUpdate.getAccessLevel(), level);
        chatToUpdate.setAccessLevel(level);

        chatService.save(chatToUpdate);
    }

    private String getLevelOfUser(User user) {
        log.debug("Request to get level of user {}", user);
        return "${command.level.userlevel} " + TextUtils.getMarkdownLinkToUser(user) + " - " + user.getAccessLevel();
    }

    private void changeUserLevel(User user, int level) {
        log.debug("Request to change level of user {} from {} to {}", user.getUsername(), user.getAccessLevel(), level);
        user.setAccessLevel(level);

        userService.save(user);
    }

    private void changeCommandLevel(CommandProperties commandProperties, int level) {
        log.debug("Request to change level of command {} from {} to {}", commandProperties.getCommandName(), commandProperties.getAccessLevel(), level);
        commandProperties.setAccessLevel(level);

        commandPropertiesService.save(commandProperties);
    }

    private String getLevelOfCommand(CommandProperties commandProperties) {
        return "${command.level.commandlevel} " + commandProperties.getCommandName() + " - " + commandProperties.getAccessLevel();
    }

}
