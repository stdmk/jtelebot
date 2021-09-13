package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.AccessLevel;
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
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@AllArgsConstructor
public class Karma implements CommandParent<SendMessage>, TextAnalyzer {

    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;
    private final ChatService chatService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final BotStats botStats;

    private final List<String> increaseSymbols = Arrays.asList("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "+1", "++");
    private final List<String> decreaseSymbols = Arrays.asList("👎🏿", "👎🏾", "👎🏽", "👎🏼", "👎🏻", "👎", "-1", "--");

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        if (message.getChatId() > 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        StringBuilder buf = new StringBuilder();
        String textMessage = cutCommandInText(message.getText());

        if (textMessage == null) {
            UserStats userStats;
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                userStats = userStatsService.get(chatService.get(message.getChatId()), userService.get(repliedMessage.getFrom().getId()));
            } else {
                userStats = userStatsService.get(chatService.get(message.getChatId()), userService.get(message.getFrom().getId()));
            }

            String karmaEmoji;
            if (userStats.getNumberOfKarma() >= 0) {
                karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getEmoji();
            } else {
                karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getEmoji();
            }

            buf.append("<b>").append(getLinkToUser(userStats.getUser(), true)).append("</b>\n")
                .append(karmaEmoji).append("Карма: <b>").append(userStats.getNumberOfKarma()).append("</b> (").append(userStats.getNumberOfAllKarma()).append(")").append("\n")
                .append(Emoji.RED_HEART.getEmoji()).append("Доброта: <b>").append(userStats.getNumberOfGoodness()).append("</b> (").append(userStats.getNumberOfAllGoodness()).append(")").append("\n")
                .append(Emoji.BROKEN_HEART.getEmoji()).append("Злобота: <b>").append(userStats.getNumberOfWickedness()).append("</b> (").append(userStats.getNumberOfAllWickedness()).append(")").append("\n");
        } else {

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

            User anotherUser;
            try {
                anotherUser = userService.get(Long.parseLong(textMessage.substring(0, i)));
            } catch (NumberFormatException e) {
                anotherUser = userService.get(textMessage.substring(0, i));
            }

            if (anotherUser == null || anotherUser.getUserId().equals(message.getFrom().getId())) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            Chat chat = chatService.get(message.getChatId());
            UserStats anotherUserStats = userStatsService.get(chat, anotherUser);
            anotherUserStats.setNumberOfKarma(anotherUserStats.getNumberOfKarma() + value);
            anotherUserStats.setNumberOfAllKarma(anotherUserStats.getNumberOfAllKarma() + value);

            User user = userService.get(message.getFrom().getId());
            UserStats userStats = userStatsService.get(chat, user);
            if (value > 0) {
                userStats.setNumberOfGoodness(userStats.getNumberOfGoodness() + 1);
                userStats.setNumberOfAllGoodness(userStats.getNumberOfAllGoodness() + 1);
            } else {
                userStats.setNumberOfWickedness(userStats.getNumberOfWickedness() + 1);
                userStats.setNumberOfAllWickedness(userStats.getNumberOfAllWickedness() + 1);
            }

            userStatsService.save(Arrays.asList(anotherUserStats, userStats));

            buf = new StringBuilder("Карма пользователя <b>" + getLinkToUser(anotherUser, true) + "</b> ");
            if (value < 0) {
                buf.append("уменьшена ").append(Emoji.THUMBS_DOWN.getEmoji());
            } else {
                buf.append("увеличена ").append(Emoji.THUMBS_UP.getEmoji());
            }
            buf.append(" до <b>").append(anotherUserStats.getNumberOfKarma()).append("</b>");
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
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
            CommandProperties commandProperties = commandPropertiesService.getCommand(this.getClass());
            AccessLevel userAccessLevel = userService.getCurrentAccessLevel(message.getFrom().getId(), message.getChatId());
            if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
                Update newUpdate = copyUpdate(update);
                if (newUpdate == null) {
                    return;
                }
                newUpdate.getMessage().setText(commandProperties.getCommandName() + " " + message.getReplyToMessage().getFrom().getId() + " " + value);

                Parser parser = new Parser(bot, command, newUpdate, botStats);
                parser.start();
            }
        }
    }
}
