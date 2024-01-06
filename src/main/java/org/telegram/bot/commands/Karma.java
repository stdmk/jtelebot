package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.telegram.bot.utils.TextUtils.startsWithElementInList;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Karma implements Command<SendMessage>, TextAnalyzer {

    private final Bot bot;
    private final ObjectCopier objectCopier;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;
    private final UserService userService;
    private final UserStatsService userStatsService;

    private final List<String> increaseSymbols = Arrays.asList("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "+1", "++");
    private final List<String> decreaseSymbols = Arrays.asList("👎🏿", "👎🏾", "👎🏽", "👎🏼", "👎🏻", "👎", "-1", "--");

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        if (message.getChatId() > 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        bot.sendTyping(message.getChatId());

        String textMessage = cutCommandInText(message.getText());
        String responseText;

        if (textMessage == null) {
            responseText = getKarmaStatsOfUser(message);
        } else {
            responseText = changeKarmaOfUser(message, textMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getKarmaStatsOfUser(Message message) {
        Message repliedMessage = message.getReplyToMessage();
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(Objects.requireNonNullElse(repliedMessage, message).getFrom().getId());

        log.debug("Request to get karma info for user {} and chat {}", user, chat);
        UserStats userStats = userStatsService.get(chat, user);

        String karmaEmoji;
        if (userStats.getNumberOfKarma() >= 0) {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getSymbol();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getSymbol();
        }

        return "<b>" + getLinkToUser(userStats.getUser(), true) + "</b>\n" +
                karmaEmoji + "${command.karma.caption}: <b>" + userStats.getNumberOfKarma() + "</b> (" + userStats.getNumberOfAllKarma() + ")" + "\n" +
                Emoji.RED_HEART.getSymbol() + "${command.karma.kindness}: <b>" + userStats.getNumberOfGoodness() + "</b> (" + userStats.getNumberOfAllGoodness() + ")" + "\n" +
                Emoji.BROKEN_HEART.getSymbol() + "${command.karma.wickedness}: <b>" + userStats.getNumberOfWickedness() + "</b> (" + userStats.getNumberOfAllWickedness() + ")" + "\n";
    }

    private String changeKarmaOfUser(Message message, String textMessage) {
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

        log.debug("Request to change karma {} of user {} ", value, anotherUser);
        Chat chat = new Chat().setChatId(message.getChatId());
        UserStats anotherUserStats = userStatsService.get(chat, anotherUser);
        anotherUserStats.setNumberOfKarma(anotherUserStats.getNumberOfKarma() + value)
                .setNumberOfKarmaPerDay(anotherUserStats.getNumberOfKarmaPerDay() + value)
                .setNumberOfAllKarma(anotherUserStats.getNumberOfAllKarma() + value);

        User user = new User().setUserId(message.getFrom().getId());
        UserStats userStats = userStatsService.get(chat, user);
        if (value > 0) {
            userStats.setNumberOfGoodness(userStats.getNumberOfGoodness() + 1)
                    .setNumberOfGoodnessPerDay(userStats.getNumberOfGoodnessPerDay() + 1)
                    .setNumberOfAllGoodness(userStats.getNumberOfAllGoodness() + 1);
        } else {
            userStats.setNumberOfWickedness(userStats.getNumberOfWickedness() + 1)
                    .setNumberOfWickednessPerDay(userStats.getNumberOfWickednessPerDay() + 1)
                    .setNumberOfAllWickedness(userStats.getNumberOfAllWickedness() + 1);
        }

        userStatsService.save(Arrays.asList(anotherUserStats, userStats));


        StringBuilder buf = new StringBuilder("${command.karma.userskarma} <b>" + getLinkToUser(anotherUser, true) + "</b> ");
        if (value < 0) {
            buf.append("${command.karma.reduced} ").append(Emoji.THUMBS_DOWN.getSymbol());
        } else {
            buf.append("${command.karma.increased} ").append(Emoji.THUMBS_UP.getSymbol());
        }
        buf.append(" ${command.karma.changedto} <b>").append(anotherUserStats.getNumberOfKarma()).append("</b>");

        return buf.toString();
    }

    @Override
    public void analyze(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();
        if (textMessage == null) {
            return;
        }

        log.debug("Initialization of searching changing karma in text {}", textMessage);

        boolean wrongNumber = true;
        try {
            Integer.parseInt(textMessage.substring(0, 2));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            wrongNumber = false;
        }
        if ((textMessage.startsWith("+") || textMessage.startsWith("-")) && (!"+1".equals(textMessage) && !"-1".equals(textMessage)) && wrongNumber) {
            log.debug("Value of karma change is too large. No karma changes");
            return;
        }

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
                Update newUpdate = objectCopier.copyObject(update, Update.class);
                if (newUpdate == null) {
                    return;
                }
                newUpdate.getMessage().setText(commandProperties.getCommandName() + " " + message.getReplyToMessage().getFrom().getId() + " " + value);
                bot.parseAsync(newUpdate, this);
            }
        }
    }
}
