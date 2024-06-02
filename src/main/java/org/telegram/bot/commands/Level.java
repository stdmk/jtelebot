package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
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
                    responseText = getLevelOfUser(commandArgument);
                }
            } else {
                String username = commandArgument.substring(0, i);
                int level;
                try {
                    level = Integer.parseInt(commandArgument.substring(i + 1));
                } catch (NumberFormatException e) {
                    log.debug("Cannot parse level in {}", commandArgument);
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                changeUserLevel(username, level);
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

    private String getLevelOfUser(String username) {
        log.debug("Request to get level of user with username {}", username);
        User user = userService.get(username);
        if (user == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return "${command.level.userlevel} " + TextUtils.getLinkToUser(user, false) + " - " + user.getAccessLevel();
    }

    private void changeUserLevel(String username, int level) {
        User userToUpdate = userService.get(username);
        if (userToUpdate == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        log.debug("Request to change level of user {} from {} to {}", username, userToUpdate.getAccessLevel(), level);
        userToUpdate.setAccessLevel(level);

        userService.save(userToUpdate);
    }

}
