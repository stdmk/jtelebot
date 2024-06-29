package org.telegram.bot.commands;

import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.TELEGRAM_MESSAGE_TEXT_MAX_LENGTH;

public interface Command {

    List<BotResponse> parse(BotRequest request);

    default List<BotResponse> returnResponse() {
        return returnResponse(null);
    }

    default List<BotResponse> returnResponse(TextResponse textResponse) {
        if (textResponse == null) {
            return Collections.emptyList();
        }

        return List.of(textResponse);
    }

    default List<BotResponse> returnResponse(BotResponse botResponse) {
        if (botResponse == null) {
            return Collections.emptyList();
        }

        return List.of(botResponse);
    }

    default List<BotResponse> mapToTextResponseList(List<String> responseTextList, Message message, ResponseSettings responseSettings) {
        List<BotResponse> result = new ArrayList<>();

        StringBuilder buf = new StringBuilder();
        for (String responseText : responseTextList) {
            if (buf.length() + responseText.length() > TELEGRAM_MESSAGE_TEXT_MAX_LENGTH) {
                result.add(buildTextResponse(buf.toString(), message, responseSettings));
                buf = new StringBuilder();
            }

            buf.append(responseText);
        }

        if (!buf.isEmpty()) {
            result.add(buildTextResponse(buf.toString(), message, responseSettings));
        }

        return result;
    }

    private TextResponse buildTextResponse(String text, Message message, ResponseSettings responseSettings) {
        return new TextResponse(message)
                .setText(text)
                .setResponseSettings(responseSettings);
    }

}
