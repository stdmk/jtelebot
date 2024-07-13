package org.telegram.bot.commands;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import java.util.List;

@Component
@Slf4j
public class Latin implements Command {

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (!message.hasCommandArgument()) {
            return returnResponse();
        }

        String responseText = checkLatin(message.getCommandArgument());

        return returnResponse(new TextResponse(message)
                .setText(responseText));
    }

    private String checkLatin(String text) {
        StringBuilder buf = new StringBuilder();
        int nonLatinCount = 0;
        boolean hasNonLatin = false;

        for (char c : text.toCharArray()) {
            String newChar = String.valueOf(c);
            if (Character.isLetter(c) && !isLatinLetter(c)) {
                if (!hasNonLatin) {
                    newChar = "→" + newChar;
                }
                nonLatinCount = nonLatinCount + 1;
                hasNonLatin = true;
            } else {
                if (hasNonLatin) {
                    newChar = "←" + newChar;
                }
                hasNonLatin = false;
            }

            buf.append(newChar);
        }

        buf.append("\n\n").append("${command.latin.nonlatincount}: ").append(nonLatinCount);

        return buf.toString();
    }

    private boolean isLatinLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

}
