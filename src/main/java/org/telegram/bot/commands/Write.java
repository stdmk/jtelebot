package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.enums.RequestSource;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.TextUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Write implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final ChatService chatService;
    private final UserStatsService userStatsService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        if (!message.hasCommandArgument()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        sendMessage(message.getCommandArgument(), message.getUser(), request.getSource());

        return returnResponse();
    }

    private void sendMessage(String commandArgument, User user, RequestSource source) {
        int spaceIndex = commandArgument.indexOf(" ");
        if (spaceIndex < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String rawId = commandArgument.substring(0, spaceIndex);
        String text = commandArgument.substring(spaceIndex + 1);

        long chatId;
        try {
            chatId = Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (chatId >= 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Chat chat = chatService.get(chatId);
        if (chat == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        List<Long> usersOfChatIds = userStatsService.getUsersOfChat(chat).stream().map(User::getUserId).toList();
        if (!usersOfChatIds.contains(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        bot.sendMessage(new TextResponse()
                .setChatId(chatId)
                .setResponseSettings(FormattingStyle.HTML)
                .setText(TextUtils.getHtmlLinkToUser(user) + " (" + source.getName() + "): " + text));
    }

}
