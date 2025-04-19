package org.telegram.bot.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.CustomReactionsStats;
import org.telegram.bot.domain.model.ReactionsStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.bot.utils.TextUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class Reactions implements Command, MessageAnalyzer {

    private static final Integer MESSAGE_TEXT_MAX_LENGTH = 50;
    private static final Integer MAX_TOP_LIST_LENGTH = 10;
    private static final Integer MAX_USER_STATS_EMOJIS_COUNT = 10;
    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setNotification(false)
            .setWebPagePreview(false);

    private final Set<String> topArgumentNames = new HashSet<>();

    private final InternationalizationService internationalizationService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageStatsService messageStatsService;
    private final ReactionsStatsService reactionsStatsService;
    private final ReactionDayStatsService reactionDayStatsService;
    private final CustomReactionDayStatsService customReactionDayStatsService;
    private final CustomReactionsStatsService customReactionsStatsService;
    private final SpeechService speechService;
    private final Bot bot;

    @PostConstruct
    private void postConstruct() {
        topArgumentNames.addAll(internationalizationService.getAllTranslations("command.reactions.argumenttop"));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        Chat chat = message.getChat();
        bot.sendTyping(chat.getChatId());
        if (TelegramUtils.isPrivateChat(chat)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        String responseText;
        if (message.hasCommandArgument()) {
            String commandArgument = message.getCommandArgument();
            if (topArgumentNames.contains(commandArgument)) {
                responseText = getTopOfUsers(chat);
            } else {
                User user = userService.get(commandArgument);
                if (user == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
                responseText = getTopOfUser(chat, user);
            }
        } else {
            responseText = getTodayTop(chat);
            if (responseText == null) {
                responseText = "${command.reactions.boringday}";
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
    }

    @Nullable
    public String getTodayTop(Chat chat) {
        LocalDate date = LocalDate.now();

        List<MessageStats> byRepliesCountTop = messageStatsService.getByRepliesCountTop(chat, date);
        List<MessageStats> byReactionsCountTop = messageStatsService.getByReactionsCountTop(chat, date);

        if (byRepliesCountTop.isEmpty() && byReactionsCountTop.isEmpty()) {
            return null;
        } else {
            List<ReactionDayStats> reactionDayStats = reactionDayStatsService.get(chat);
            List<CustomReactionDayStats> customReactionDayStats = customReactionDayStatsService.get(chat);
            return buildResponseText(byRepliesCountTop, byReactionsCountTop, chat.getChatId()) + "\n"
                    + getTopOfUsersByDay(reactionDayStats, customReactionDayStats);
        }
    }

    private String getTopOfUsers(Chat chat) {
        ReactionsStats reactionsStats = reactionsStatsService.get(chat);
        CustomReactionsStats customReactionsStats = customReactionsStatsService.get(chat);

        return getTopOfUsersByDay(reactionsStats.getReactionDayStatsList(), customReactionsStats.getCustomReactionDayStats()) + "\n"
                + getTopOfUsersByMonth(reactionsStats.getReactionMonthStatsList(), customReactionsStats.getCustomReactionMonthStats()) + "\n"
                + getTopOfUsersByAll(reactionsStats.getReactionStatsList(), customReactionsStats.getCustomReactionStats()) + "\n";
    }

    private String getTopOfUsersByDay(List<ReactionDayStats> reactionDayStatsList, List<CustomReactionDayStats> customReactionDayStatsList) {
        if (reactionDayStatsList.isEmpty() && customReactionDayStatsList.isEmpty()) {
            return "";
        }

        Map<User, Stats> userDayStatsMap = new HashMap<>();
        reactionDayStatsList.forEach(reactionDayStats -> {
            Stats stats = userDayStatsMap.get(reactionDayStats.getUser());
            if (stats == null) {
                stats = new Stats();
            }

            stats.getEmojiCountMap().put(reactionDayStats.getEmoji(), reactionDayStats.getCount());

            userDayStatsMap.put(reactionDayStats.getUser(), stats);
        });
        customReactionDayStatsList.forEach(customReactionDayStats -> {
            Stats stats = userDayStatsMap.get(customReactionDayStats.getUser());
            if (stats == null) {
                stats = new Stats();
            }

            stats.setCustomEmojiCount(stats.getCustomEmojiCount() + customReactionDayStats.getCount());
        });
        userDayStatsMap.values().forEach(Stats::calculateTotal);

        return buildTopString(userDayStatsMap, "${command.reactions.caption.byday}");
    }

    private String getTopOfUsersByMonth(List<ReactionMonthStats> reactionMonthStatsList, List<CustomReactionMonthStats> customReactionMonthStatsList) {
        Map<User, Stats> userMonthStatsMap = new HashMap<>();
        reactionMonthStatsList.forEach(reactionMonthStats -> {
            Stats stats = userMonthStatsMap.get(reactionMonthStats.getUser());
            if (stats == null) {
                stats = new Stats();
            }

            stats.getEmojiCountMap().put(reactionMonthStats.getEmoji(), reactionMonthStats.getCount());

            userMonthStatsMap.put(reactionMonthStats.getUser(), stats);
        });
        customReactionMonthStatsList.forEach(customReactionMonthStats -> {
            Stats stats = userMonthStatsMap.get(customReactionMonthStats.getUser());
            if (stats == null) {
                stats = new Stats();
            }

            stats.setCustomEmojiCount(stats.getCustomEmojiCount() + customReactionMonthStats.getCount());
        });
        userMonthStatsMap.values().forEach(Stats::calculateTotal);

        return buildTopString(userMonthStatsMap, "${command.reactions.caption.bymonth}");
    }

    private String getTopOfUsersByAll(List<ReactionStats> reactionsStats, List<CustomReactionStats> customReactionsStats) {
        Map<User, Stats> userStatsMap = new HashMap<>();
        reactionsStats.forEach(reactionStats -> {
            Stats stats = userStatsMap.get(reactionStats.getUser());
            if (stats == null) {
                stats = new Stats();
            }

            stats.getEmojiCountMap().put(reactionStats.getEmoji(), reactionStats.getCount());

            userStatsMap.put(reactionStats.getUser(), stats);
        });
        customReactionsStats.forEach(customReactionStats -> {
            Stats stats = userStatsMap.get(customReactionStats.getUser());
            if (stats == null) {
                stats = new Stats();
            }

            stats.setCustomEmojiCount(stats.getCustomEmojiCount() + customReactionStats.getCount());
        });
        userStatsMap.values().forEach(Stats::calculateTotal);

        return buildTopString(userStatsMap, "${command.reactions.caption.byall}");
    }

    private String buildTopString(Map<User, Stats> userStatsMap, String caption) {
        int maxEmojisValueLength = userStatsMap.values()
                .stream()
                .mapToInt(stats -> stats.getEmojiCountMap().values().stream().mapToInt(count -> count).max().orElse(1))
                .mapToObj(String::valueOf)
                .mapToInt(String::length)
                .max()
                .orElse(1);
        int maxLengthOfPlace = userStatsMap.size() >= MAX_TOP_LIST_LENGTH ? 3 : 2;

        StringBuilder buf = new StringBuilder();
        buf.append("<u>").append(caption).append(":</u>\n");
        AtomicInteger i = new AtomicInteger(0);
        userStatsMap.entrySet()
                .stream()
                .filter(userStatsEntry -> userStatsEntry.getValue().getTotal() != 0)
                .sorted(Comparator.comparing((Map.Entry<User, Stats> userStats) -> userStats.getValue().getTotal()).reversed())
                .limit(MAX_TOP_LIST_LENGTH)
                .forEach(userStatsEntry -> {
                    User user = userStatsEntry.getKey();
                    Stats stats = userStatsEntry.getValue();

                    String popularEmojis = stats.getEmojiCountMap().entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(MAX_USER_STATS_EMOJIS_COUNT)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.joining());
                    buf.append("<code>").append(String.format("%-" + maxLengthOfPlace + "s %-" + maxEmojisValueLength + "d %-20s", i.incrementAndGet() + ")", stats.getTotal(), TextUtils.getHtmlLinkToUser(user))).append("</code>\n")
                            .append(popularEmojis).append("\n");
                });

        return buf.toString();
    }

    private String getTopOfUser(Chat chat, User user) {
        ReactionsStats reactionsStats = reactionsStatsService.get(chat, user);
        CustomReactionsStats customReactionsStats = customReactionsStatsService.get(chat, user);

        int totalByDayCustom = customReactionsStats.getCustomReactionDayStats().stream().mapToInt(CustomReactionDayStats::getCount).sum();
        int totalByMonthCustom = customReactionsStats.getCustomReactionMonthStats().stream().mapToInt(CustomReactionMonthStats::getCount).sum();
        int totalByAllCustom = customReactionsStats.getCustomReactionStats().stream().mapToInt(CustomReactionStats::getCount).sum();

        String totalByDayCustomString = totalByDayCustom == 0 ? "" : "(" + totalByDayCustom + " ${command.reactions.customreaction})";
        String totalByMonthCustomString = totalByMonthCustom == 0 ? "" : "(" + totalByMonthCustom + " ${command.reactions.customreaction})";
        String totalByAllCustomString = totalByAllCustom == 0 ? "" : "(" + totalByAllCustom + " ${command.reactions.customreaction})";

        int totalByDay = reactionsStats.getReactionDayStatsList().stream().mapToInt(ReactionDayStats::getCount).sum() + totalByDayCustom;
        int totalByMonth = reactionsStats.getReactionMonthStatsList().stream().mapToInt(ReactionMonthStats::getCount).sum() + totalByMonthCustom;
        int totalByAll = reactionsStats.getReactionStatsList().stream().mapToInt(ReactionStats::getCount).sum() + totalByAllCustom;

        String popularDayEmojis = reactionsStats.getReactionDayStatsList().stream().sorted(Comparator.comparing(ReactionDayStats::getCount).reversed()).limit(MAX_USER_STATS_EMOJIS_COUNT).map(ReactionDayStats::getEmoji).collect(Collectors.joining());
        String popularMonthEmojis = reactionsStats.getReactionMonthStatsList().stream().sorted(Comparator.comparing(ReactionMonthStats::getCount).reversed()).limit(MAX_USER_STATS_EMOJIS_COUNT).map(ReactionMonthStats::getEmoji).collect(Collectors.joining());
        String popularAllEmojis = reactionsStats.getReactionStatsList().stream().sorted(Comparator.comparing(ReactionStats::getCount).reversed()).limit(MAX_USER_STATS_EMOJIS_COUNT).map(ReactionStats::getEmoji).collect(Collectors.joining());

        return "<b>" + TextUtils.getHtmlLinkToUser(user) + ":</b>\n"
                + "<u>${command.reactions.caption.byday}:</u> " + totalByDay + totalByDayCustomString + "\n"
                + popularDayEmojis + "\n"
                + "<u>${command.reactions.caption.bymonth}:</u> " + totalByMonth + totalByMonthCustomString + "\n"
                + popularMonthEmojis + "\n"
                + "<u>${command.reactions.caption.byall}:</u> " + totalByAll + totalByAllCustomString + "\n"
                + popularAllEmojis + "\n";
    }

    private String buildResponseText(List<MessageStats> byRepliesCountTop, List<MessageStats> byReactionsCountTop, Long chatId) {
        StringBuilder buf = new StringBuilder("<b>${command.reactions.caption}:</b>\n");

        if (!byRepliesCountTop.isEmpty()) {
            buf.append("<u>${command.reactions.byreplies}:</u>\n");
            int i = 1;
            for (MessageStats messageStats : byRepliesCountTop) {
                buf.append(i).append(") ").append(buildByRepliesResponseString(messageStats, chatId));
                i = i + 1;
            }
        }

        buf.append("\n");

        if (!byReactionsCountTop.isEmpty()) {
            buf.append("<u>${command.reactions.byreactions}:</u>\n");
            int i = 1;
            for (MessageStats messageStats : byReactionsCountTop) {
                buf.append(i).append(") ").append(buildByReactionsResponseString(messageStats, chatId));
                i = i + 1;
            }
        }

        return buf.toString();
    }

    private String buildByRepliesResponseString(MessageStats messageStats, Long chatId) {
        return buildResponseString(messageStats.getReplies(), messageStats.getMessage(), chatId);
    }

    private String buildByReactionsResponseString(MessageStats messageStats, Long chatId) {
        return buildResponseString(messageStats.getReactions(), messageStats.getMessage(), chatId);
    }

    private String buildResponseString(int count, org.telegram.bot.domain.entities.Message message, Long chatId) {
        String messageText = message.getText();
        if (messageText == null) {
            messageText = message.getMessageContentType().getName();
        } else {
            messageText = TextUtils.getLessThanCount(TextUtils.reduceSpaces(messageText), MESSAGE_TEXT_MAX_LENGTH);
        }

        return TextUtils.getHtmlLinkToMessage(chatId, message.getMessageId(), messageText) + " (" + count + ")\n";
    }

    @Override
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();
        if (shouldNotBeAnalyzed(message)) {
            return returnResponse();
        }

        if (message.hasReplyToMessage()) {
            org.telegram.bot.domain.entities.Message replyToMessage = messageService.get(message.getReplyToMessage().getMessageId());
            if (replyToMessage != null) {
                messageStatsService.incrementReplies(replyToMessage);
            }
        } else if (message.hasReactions()) {
            org.telegram.bot.domain.entities.Message reactionsToMessage = messageService.get(message.getMessageId());
            if (reactionsToMessage == null || message.getUser().getUserId().equals(reactionsToMessage.getUser().getUserId())) {
                return returnResponse();
            }

            org.telegram.bot.domain.model.request.Reactions reactions = message.getReactions();
            List<String> newEmojis = reactions.getNewEmojis();
            List<String> newCustomEmojisIds = reactions.getNewCustomEmojisIds();
            List<String> oldEmojis = reactions.getOldEmojis();
            List<String> oldCustomEmojisIds = reactions.getOldCustomEmojisIds();
            int reactionsCount = (newEmojis.size() + newCustomEmojisIds.size()) - (oldEmojis.size() + oldCustomEmojisIds.size());
            messageStatsService.incrementReactions(reactionsToMessage, reactionsCount);

            Chat chat = message.getChat();
            User recepientUser = reactionsToMessage.getUser();
            reactionsStatsService.update(chat, recepientUser, oldEmojis, newEmojis);
            customReactionsStatsService.update(chat, recepientUser, oldCustomEmojisIds, newCustomEmojisIds);
        }

        return returnResponse();
    }

    private boolean shouldNotBeAnalyzed(Message message) {
        return TelegramUtils.isPrivateChat(message.getChat())
                || MessageKind.CALLBACK.equals(message.getMessageKind())
                || MessageKind.EDIT.equals(message.getMessageKind());
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class Stats {
        private Map<String, Integer> emojiCountMap = new HashMap<>();
        private long customEmojiCount = 0;
        private long total;

        public void calculateTotal() {
            this.total = this.emojiCountMap.values().stream().mapToLong(count -> count).sum() + customEmojiCount;
        }
    }

}
