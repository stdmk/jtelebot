package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Getid implements Command<SendMessage> {

    private final Bot bot;
    private final UserService userService;
    private final SpeechService speechService;

    @Override
    public List<SendMessage> parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = getTextMessage(update);
        StringBuilder responseText = new StringBuilder();
        Long chatId = message.getChatId();

        if (textMessage != null) {
            log.debug("Request to getting telegram id of {}", textMessage);
            User user = userService.get(textMessage);
            if (user == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            responseText.append("${command.getid.id} ").append(getLinkToUser(user, false)).append(": `").append(user.getUserId()).append("`\n");
        }

        Message repliedMessage = message.getReplyToMessage();
        if (repliedMessage != null) {
            org.telegram.telegrambots.meta.api.objects.User repliedUser = repliedMessage.getFrom();
            log.debug("Request to getting telegram id of {}", repliedUser);
            User user = userService.get(repliedUser.getId());
            responseText.append("${command.getid.id} ").append(getLinkToUser(user, false)).append(": `").append(user.getUserId()).append("`\n");
        }

        if (chatId < 0) {
            log.debug("Request to getting telegram id of public chat {}", message.getChat());
            responseText.append("${command.getid.groupid}: `").append(chatId).append("`\n");
        }

        responseText.append("${command.getid.yourid}: `").append(message.getFrom().getId()).append("`");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText.toString());

        return returnOneResult(sendMessage);
    }
}
