package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.telegram.bot.utils.TextUtils.cutCommandInText;

@Component
@AllArgsConstructor
public class Level extends CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Level.class);

    private final UserService userService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        String textMessage = cutCommandInText(update.getMessage().getText());
        int i;
        if (textMessage == null) {
            Long chatId = update.getMessage().getChatId();
            if (chatId > 0) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }
            return new SendMessage()
                    .setChatId(chatId)
                    .setReplyToMessageId(update.getMessage().getMessageId())
                    .setText("Уровень этого чата - " + chatService.getChatAccessLevel(chatId));
        } else {
            i = textMessage.indexOf(" ");
        }
        int level;
        String username = null;

        try {
            if (i < 0) {
                level = Integer.parseInt(textMessage);
            } else {
                username = textMessage.substring(0, i);
                level = Integer.parseInt(textMessage.substring(i + 1));
            }
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        if (username != null) {
            User userToUpdate = userService.get(username);
            if (userToUpdate == null) {
                throw new BotException(speechService.getRandomMessageByTag("userNotFount"));
            }

            log.debug("Request to change level of user {} from {} to {}", username, userToUpdate.getAccessLevel(), level);
            userToUpdate.setAccessLevel(level);
            userService.save(userToUpdate);

            return new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setText(speechService.getRandomMessageByTag("saved"));
        } else {
            org.telegram.bot.domain.entities.Chat chatToUpdate = chatService.get(update.getMessage().getChatId());

            log.debug("Request to change level of chat {} from {} to {}", chatToUpdate.getChatId(), chatToUpdate.getAccessLevel(), level);
            chatToUpdate.setAccessLevel(level);
            chatService.save(chatToUpdate);

            return new SendMessage().setReplyToMessageId(update.getMessage().getMessageId())
                    .setChatId(update.getMessage().getChatId())
                    .setText(speechService.getRandomMessageByTag("saved"));
        }
    }
}
