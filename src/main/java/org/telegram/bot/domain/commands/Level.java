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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Level implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Level.class);

    private final UserService userService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        int i;
        if (textMessage == null) {
            Long chatId = message.getChatId();
            if (chatId > 0) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableMarkdown(true);
            sendMessage.setText("Уровень этого чата - " + chatService.getChatAccessLevel(chatId));

            return sendMessage;
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

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText(speechService.getRandomMessageByTag("saved"));

            return sendMessage;
        } else {
            org.telegram.bot.domain.entities.Chat chatToUpdate = chatService.get(message.getChatId());

            log.debug("Request to change level of chat {} from {} to {}", chatToUpdate.getChatId(), chatToUpdate.getAccessLevel(), level);
            chatToUpdate.setAccessLevel(level);
            chatService.save(chatToUpdate);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText(speechService.getRandomMessageByTag("saved"));

            return sendMessage;
        }
    }
}
