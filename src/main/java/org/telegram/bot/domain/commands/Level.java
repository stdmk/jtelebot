package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Level implements CommandParent<SendMessage> {

    private final Bot bot;
    private final UserService userService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        if (textMessage == null) {
            Long chatId = message.getChatId();
            log.debug("Request to get level of chat with id {}", chatId);

            if (chatId > 0) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = "Уровень этого чата - " + chatService.getChatAccessLevel(chatId);
        } else {
            int i = textMessage.indexOf(" ");
            int level;

            if (i < 0) {
                try {
                    level = Integer.parseInt(textMessage);
                    org.telegram.bot.domain.entities.Chat chatToUpdate = chatService.get(message.getChatId());

                    log.debug("Request to change level of chat {} from {} to {}", chatToUpdate.getChatId(), chatToUpdate.getAccessLevel(), level);
                    chatToUpdate.setAccessLevel(level);

                    chatService.save(chatToUpdate);
                    responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
                } catch (NumberFormatException e) {
                    log.debug("Request to get level of user with username {}", textMessage);
                    User user = userService.get(textMessage);
                    if (user == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }

                    responseText = "Уровень " + TextUtils.getLinkToUser(user, false) + " - " + user.getAccessLevel();
                }
            } else {
                String username = textMessage.substring(0, i);
                try {
                    level = Integer.parseInt(textMessage.substring(i + 1));
                } catch (NumberFormatException e) {
                    log.debug("Cannot parse level in {}", textMessage);
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
                User userToUpdate = userService.get(username);
                if (userToUpdate == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                log.debug("Request to change level of user {} from {} to {}", username, userToUpdate.getAccessLevel(), level);
                userToUpdate.setAccessLevel(level);

                userService.save(userToUpdate);
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }
}
