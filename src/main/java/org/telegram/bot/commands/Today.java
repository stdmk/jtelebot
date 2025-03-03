package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.MessageStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.MessageStatsService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.bot.utils.TextUtils;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class Today implements Command, MessageAnalyzer {

    private static final Integer MESSAGE_TEXT_MAX_LENGTH = 50;
    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setNotification(false)
            .setWebPagePreview(false);

    private final MessageStatsService messageStatsService;
    private final SpeechService speechService;
    private final Bot bot;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        Chat chat = message.getChat();
        bot.sendTyping(chat.getChatId());
        if (TelegramUtils.isPrivateChat(chat)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        String responseText = getResponseText(chat);
        if (responseText == null) {
            responseText = "${command.today.boringday}";
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
    }

    @Nullable
    public String getResponseText(Chat chat) {
        LocalDate date = LocalDate.now();

        List<MessageStats> byRepliesCountTop = messageStatsService.getByRepliesCountTop(chat, date);
        List<MessageStats> byReactionsCountTop = messageStatsService.getByReactionsCountTop(chat, date);

        if (byRepliesCountTop.isEmpty() && byReactionsCountTop.isEmpty()) {
            return null;
        } else {
            return buildResponseText(byRepliesCountTop, byReactionsCountTop, chat.getChatId());
        }
    }

    private String buildResponseText(List<MessageStats> byRepliesCountTop, List<MessageStats> byReactionsCountTop, Long chatId) {
        StringBuilder buf = new StringBuilder("<b>${command.today.caption}:</b>\n");

        if (!byRepliesCountTop.isEmpty()) {
            buf.append("<u>${command.today.byreplies}:</u>\n");
            int i = 1;
            for (MessageStats messageStats : byRepliesCountTop) {
                buf.append(i).append(") ").append(buildByRepliesResponseString(messageStats, chatId));
                i = i + 1;
            }
        }

        buf.append("\n");

        if (!byReactionsCountTop.isEmpty()) {
            buf.append("<u>${command.today.byreactions}:</u>\n");
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
        return TextUtils.getHtmlLinkToMessage(
                        chatId,
                        message.getMessageId(),
                        TextUtils.getLessThanCount(message.getText(), MESSAGE_TEXT_MAX_LENGTH))
                + " (" + count + ")\n";
    }

    @Override
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();

        if (!TelegramUtils.isPrivateChat(message.getChat())) {
            if (message.hasReplyToMessage()) {
                messageStatsService.incrementReplies(message.getReplyToMessage().getMessageId());
            } else if (message.hasReactions()) {
                messageStatsService.setReactions(message.getMessageId(), message.getReactionsCount() - 1);
            }
        }

        return returnResponse();
    }

}
