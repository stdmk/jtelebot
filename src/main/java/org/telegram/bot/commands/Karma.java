package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.bot.utils.TelegramUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.telegram.bot.utils.TextUtils.getHtmlLinkToUser;
import static org.telegram.bot.utils.TextUtils.startsWithElementInList;

@Component
@RequiredArgsConstructor
@Slf4j
public class Karma implements Command, MessageAnalyzer {

    private final Bot bot;
    private final ObjectCopier objectCopier;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;
    private final UserService userService;
    private final UserStatsService userStatsService;

    private static final List<String> INCREASE_SYMBOLS = Arrays.asList("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "+1", "++");
    private static final List<String> DECREASE_SYMBOLS = Arrays.asList("👎🏿", "👎🏾", "👎🏽", "👎🏼", "👎🏻", "👎", "-1", "--");

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        if (TelegramUtils.isPrivateChat(message.getChat())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        String commandArgument = message.getCommandArgument();
        String responseText;
        if (commandArgument == null) {
            responseText = getKarmaStatsOfUser(message);
        } else {
            UserAndKarmaValue userAndKarmaValue = getUserAndKarmaValue(commandArgument, message.getUser().getUserId());
            if (userAndKarmaValue.value == null) {
                responseText = getKarmaStatsOfUser(message, userAndKarmaValue.user);
            } else {
                responseText = changeKarmaOfUser(message, userAndKarmaValue);
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private UserAndKarmaValue getUserAndKarmaValue(String textMessage, Long currentUserId) {
        int i = textMessage.indexOf(" ");
        if (i < 0) {
            return new UserAndKarmaValue(getUser(textMessage, currentUserId), null);
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

        return new UserAndKarmaValue(getUser(textMessage.substring(0, i), currentUserId), value);
    }

    private User getUser(String raw, Long currentUserId) {
        User anotherUser;
        try {
            anotherUser = userService.get(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            anotherUser = userService.get(raw);
        }

        if (anotherUser == null || anotherUser.getUserId().equals(currentUserId)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return anotherUser;
    }

    private String getKarmaStatsOfUser(Message message) {
        return getKarmaStatsOfUser(message, null);
    }

    private String getKarmaStatsOfUser(Message message, User user) {
        Message repliedMessage = message.getReplyToMessage();
        Chat chat = message.getChat();
        if (user == null) {
            user = Objects.requireNonNullElse(repliedMessage, message).getUser();
        }

        log.debug("Request to get karma info for user {} and chat {}", user, chat);
        UserStats userStats = userStatsService.get(chat, user);

        String karmaEmoji;
        if (userStats.getNumberOfKarma() >= 0) {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getSymbol();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getSymbol();
        }

        return "<b>" + getHtmlLinkToUser(userStats.getUser()) + "</b>\n" +
                karmaEmoji + "${command.karma.caption}: <b>" + userStats.getNumberOfKarma() + "</b> (" + userStats.getNumberOfAllKarma() + ")" + "\n" +
                Emoji.RED_HEART.getSymbol() + "${command.karma.kindness}: <b>" + userStats.getNumberOfGoodness() + "</b> (" + userStats.getNumberOfAllGoodness() + ")" + "\n" +
                Emoji.BROKEN_HEART.getSymbol() + "${command.karma.wickedness}: <b>" + userStats.getNumberOfWickedness() + "</b> (" + userStats.getNumberOfAllWickedness() + ")" + "\n";
    }

    private String changeKarmaOfUser(Message message, UserAndKarmaValue userAndKarmaValue) {
        User anotherUser = userAndKarmaValue.user;
        Integer value = userAndKarmaValue.value;
        log.debug("Request to change karma {} of user {} ", value, anotherUser);
        Chat chat = new Chat().setChatId(message.getChatId());
        UserStats anotherUserStats = userStatsService.get(chat, anotherUser);
        anotherUserStats.setNumberOfKarma(anotherUserStats.getNumberOfKarma() + value)
                .setNumberOfKarmaPerDay(anotherUserStats.getNumberOfKarmaPerDay() + value)
                .setNumberOfAllKarma(anotherUserStats.getNumberOfAllKarma() + value);

        User user = new User().setUserId(message.getUser().getUserId());
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


        StringBuilder buf = new StringBuilder("${command.karma.userskarma} <b>" + getHtmlLinkToUser(anotherUser) + "</b> ");
        if (value < 0) {
            buf.append("${command.karma.reduced} ").append(Emoji.THUMBS_DOWN.getSymbol());
        } else {
            buf.append("${command.karma.increased} ").append(Emoji.THUMBS_UP.getSymbol());
        }
        buf.append(" ${command.karma.changedto} <b>").append(anotherUserStats.getNumberOfKarma()).append("</b>");

        return buf.toString();
    }

    @Override
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();
        String textMessage = message.getText();
        if (textMessage == null || message.getReplyToMessage() == null) {
            return returnResponse();
        }

        int value = 0;
        if (startsWithElementInList(textMessage, INCREASE_SYMBOLS)) {
            value = 1;
        } else if (startsWithElementInList(textMessage, DECREASE_SYMBOLS)) {
            value = -1;
        }

        if (value != 0) {
            CommandProperties commandProperties = commandPropertiesService.getCommand(this.getClass());
            AccessLevel userAccessLevel = userService.getCurrentAccessLevel(message.getUser().getUserId(), message.getChatId());
            if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), commandProperties.getAccessLevel())) {
                BotRequest newRequest = objectCopier.copyObject(request, BotRequest.class);
                if (newRequest == null) {
                    return returnResponse();
                }
                newRequest.getMessage().setText(commandProperties.getCommandName() + " " + message.getReplyToMessage().getUser().getUserId() + " " + value);
                return this.parse(newRequest);
            }
        }

        return returnResponse();
    }

    private record UserAndKarmaValue(User user, Integer value) {
    }

}
