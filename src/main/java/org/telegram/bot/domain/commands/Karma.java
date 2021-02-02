package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.startsWithElementInList;

@Component
@AllArgsConstructor
public class Karma implements CommandParent<SendMessage>, TextAnalyzer {

    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;
    private final ChatService chatService;
    private final UserService userService;
    private final UserStatsService userStatsService;

    private final List<String> increaseSymbols = Arrays.asList(Emoji.THUMBS_UP.getEmoji(), "++");
    private final List<String> decreaseSymbols = Arrays.asList(Emoji.THUMBS_DOWN.getEmoji(), "--");

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        if (message.getChatId() > 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }
        String textMessage = cutCommandInText(message.getText());

        int i = textMessage.indexOf(" ");
        if (i < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        int value;
        try {
            value = Integer.parseInt(textMessage.substring(i + 1));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (value != 1 && value != -1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        User user;
        try {
            user = userService.get(Integer.parseInt(textMessage.substring(0, i)));
        } catch (NumberFormatException e) {
            user = userService.get(textMessage.substring(0, i));
        }

        if (user == null || user.getUserId().equals(message.getFrom().getId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserStats userStats = userStatsService.get(chatService.get(message.getChatId()), user);
        userStats.setKarma(userStats.getKarma() + value);
        userStatsService.save(userStats);

        StringBuilder buf = new StringBuilder("Карма пользователя *@" + user.getUsername() + "* ");
        if (value < 0) {
            buf.append("уменьшена ").append(Emoji.THUMBS_DOWN.getEmoji());
        } else {
            buf.append("увеличена ").append(Emoji.THUMBS_UP.getEmoji());
        }
        buf.append(" до *").append(userStats.getKarma()).append("*");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(buf.toString());

        return sendMessage;
    }

    @Override
    public void analyze(Bot bot, CommandParent<?> command, Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();
        int value = 0;

        if (startsWithElementInList(textMessage, increaseSymbols)) {
            value = 1;
        } else if (startsWithElementInList(textMessage, decreaseSymbols)) {
            value = -1;
        }

        if (value != 0 && message.getReplyToMessage() != null) {
            String commandName = commandPropertiesService.getCommand(Karma.class).getCommandName();
            update.getMessage().setText(commandName + " " + message.getReplyToMessage().getFrom().getId() + " " + value);

            Parser parser = new Parser(bot, command, update);
            parser.start();
        }
    }
}
