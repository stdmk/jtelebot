package org.telegram.bot.commands;

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
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.MathUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Password implements Command {

    private static final String UPPER_LATIN = "QWERTYUIOPASDFGHJKLZXCVBNM";
    private static final String LOWER_LATIN = "qwertyuiopasdfghjklzxcvbnm";
    private static final String DIGITS = "1234567890";
    private static final String SPECIAL_SYMBOLS = "!@$%^&*()_-+";
    private static final String SYMBOLS_SOURCE = UPPER_LATIN + LOWER_LATIN + DIGITS + SPECIAL_SYMBOLS;
    private static final Integer DEFAULT_PASSWORD_LENGTH = 15;

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        int passwordLength;
        String commandArgument = message.getCommandArgument();
        if (commandArgument != null) {
            try {
                passwordLength = Integer.parseInt(commandArgument);
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else {
            passwordLength = DEFAULT_PASSWORD_LENGTH;
        }

        if (passwordLength < 4 || passwordLength > 4096) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        log.debug("Request to generate password with {} symbols", passwordLength);

        String password = generatePassword(passwordLength);
        while (!isComplexity(password)) {
            password = generatePassword(passwordLength);
        }

        return returnResponse(new TextResponse(message)
                .setText("`" + password + "`")
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private String generatePassword(int length) {
        StringBuilder buf = new StringBuilder();
        int sourceLength = SYMBOLS_SOURCE.length();

        for (int i = 0; i < length; i++) {
            Integer position = MathUtils.getRandomInRange(0, sourceLength);
            buf.append(SYMBOLS_SOURCE.charAt(position));
        }

        return buf.toString();
    }

    private boolean isComplexity(String password) {
        return contains(UPPER_LATIN, password)
                && contains(LOWER_LATIN, password)
                && contains(DIGITS, password)
                && contains(SPECIAL_SYMBOLS, password);
    }

    private boolean contains(String source, String password) {
        for (char sourceChar : source.toCharArray()) {
            for (char passwordChar : password.toCharArray()) {
                if (sourceChar == passwordChar) {
                    return true;
                }
            }
        }

        return false;
    }

}
