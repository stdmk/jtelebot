package org.telegram.bot.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneNumber implements Command {

    private static final String API_URL = "http://rosreestr.subnets.ru/?get=num&num=";

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String responseText;
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.phonenumber.commandwaitingstart}";
        } else {
            PhoneInfo phoneInfo = getPhoneInfo(commandArgument);
            responseText = "<b>${command.phonenumber.operator}:</b> " + phoneInfo.getOperator() + "\n" +
                           "<b>${command.phonenumber.region}:</b> " + phoneInfo.getRegion();
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private PhoneInfo getPhoneInfo(String text) {
        String rawResponse;
        try {
            rawResponse = networkUtils.readStringFromURL(API_URL + text);
        } catch (FileNotFoundException fnf) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        } catch (IOException e) {
            log.error("Error from api:", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return parsePhoneInfoData(rawResponse);
    }

    private PhoneInfo parsePhoneInfoData(String rawResponse) {
        final String regionField = "region: ";
        final String operatorField = "operator: ";
        final String movedToField = "moved2operator: ";

        String[] lines = rawResponse.split("\\r?\\n");

        String operator = getValueOfLine(movedToField, Arrays.stream(lines)
                .filter(line -> line.startsWith(movedToField))
                .findFirst()
                .orElse(null));
        if (operator == null) {
            operator = getValueOfLine(operatorField, Arrays.stream(lines)
                    .filter(line -> line.startsWith(operatorField))
                    .findFirst()
                    .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING))));
        }

        String region = getValueOfLine(
                regionField,
                Arrays.stream(lines)
                        .filter(line -> line.startsWith(regionField))
                        .findFirst()
                        .orElse(null));

        return new PhoneInfo(operator, region);
    }

    private String getValueOfLine(String field, String line) {
        if (line == null) {
            return null;
        }
        return line.substring(field.length());
    }

    @Data
    @AllArgsConstructor
    private static class PhoneInfo {
        private String operator;
        private String region;
    }
}
