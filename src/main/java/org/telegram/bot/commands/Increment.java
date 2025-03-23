package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.IncrementService;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class Increment implements Command {

    private final SpeechService speechService;
    private final IncrementService incrementService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        String responseText;
        if (message.hasCommandArgument()) {
            String commandArgument = message.getCommandArgument();
            if (commandArgument.contains(" ")) {
                responseText = increment(chat, user, commandArgument);
            } else {
                responseText = getIncrementInfo(chat, user, commandArgument);
            }
        } else {
            responseText = getIncrementsInfo(chat, user);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private String increment(Chat chat, User user, String command) {
        int spaceIndex = command.indexOf(" ");
        String incrementName = command.substring(0, spaceIndex);

        BigDecimal incrementValue;
        try {
            incrementValue = new BigDecimal(command.substring(spaceIndex + 1).replace(",", "."));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        validateIncrementValue(incrementValue);

        String response;
        org.telegram.bot.domain.entities.Increment increment = incrementService.get(chat, user, incrementName);
        if (increment == null) {
            response = "${command.increment.new}" + " <b>" + incrementName + "</b>: ${command.increment.withvalue} <b>" + incrementValue.toPlainString() + "</b>";
            increment = new org.telegram.bot.domain.entities.Increment()
                    .setChat(chat)
                    .setUser(user)
                    .setName(incrementName.toLowerCase(Locale.ROOT))
                    .setCount(incrementValue);
        } else {
            BigDecimal oldCount = increment.getCount();
            BigDecimal newCount;
            try {
                newCount = oldCount.add(incrementValue);
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            String operation;
            if (incrementValue.signum() < 0) {
                operation = "${command.increment.reduced}";
            } else {
                operation = "${command.increment.increased}";
            }

            response = "${command.increment.increment} " + "<b>" + incrementName + "</b> " + operation + " ${command.increment.to} <b>" + newCount.toPlainString() + "</b>";

            increment.setCount(newCount);
        }

        incrementService.save(increment);

        return response;
    }

    private void validateIncrementValue(BigDecimal value) {
        if (BigDecimal.ZERO.equals(value)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String getIncrementInfo(Chat chat, User user, String incrementName) {
        org.telegram.bot.domain.entities.Increment increment = incrementService.get(chat, user, incrementName);
        if (increment == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return buildIncrementInfo(increment);
    }

    private String getIncrementsInfo(Chat chat, User user) {
        return "<b><u>${command.increment.incrementsinfo}</u></b>:\n" + incrementService.get(chat, user).stream().map(this::buildIncrementInfo).collect(Collectors.joining("\n"));
    }

    private String buildIncrementInfo(org.telegram.bot.domain.entities.Increment increment) {
        return "<b>" + increment.getName() + ":</b> " + increment.getCount().toPlainString();
    }

}
