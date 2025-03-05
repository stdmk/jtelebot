package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.DeleteResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.bot.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class Me implements Command {

    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        List<BotResponse> result = new ArrayList<>(2);
        Message message = request.getMessage();

        if (TelegramUtils.isPrivateChat(message.getChat())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        String commandArgument = message.getCommandArgument();
        if (commandArgument != null) {
            result.add(new TextResponse()
                    .setChatId(message.getChatId())
                    .setText("* " + TextUtils.getHtmlLinkToUser(message.getUser()) + " " + commandArgument)
                    .setResponseSettings(FormattingStyle.HTML));
        }

        result.add(new DeleteResponse(message));

        return result;
    }
}
