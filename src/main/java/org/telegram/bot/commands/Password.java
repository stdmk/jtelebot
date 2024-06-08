package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.Value;
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
import org.telegram.bot.utils.TextUtils;

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
    private static final int MAX_GENERATION_ATTEMTS = 50;

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        PasswordParams passwordParams;
        String commandArgument = message.getCommandArgument();
        if (commandArgument != null) {
            if (!isLooksLikePasswordCommandArgument(commandArgument)) {
                return returnResponse();
            }
            passwordParams = getPasswordParams(commandArgument);
        } else {
            passwordParams = new PasswordParams(DEFAULT_PASSWORD_LENGTH, SYMBOLS_SOURCE);
        }

        bot.sendTyping(message.getChatId());
        log.debug("Request to generate password by params {}", passwordParams);

        String password = generatePassword(passwordParams);
        int attempts = 1;
        while (!isComplexity(password)) {
            if (attempts > MAX_GENERATION_ATTEMTS) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            password = generatePassword(passwordParams);
            attempts = attempts + 1;
        }

        return returnResponse(new TextResponse(message)
                .setText("`" + password + "`")
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private boolean isLooksLikePasswordCommandArgument(String argument) {
        return TextUtils.isThatInteger(argument.substring(0, 1)) || argument.length() <= SPECIAL_SYMBOLS.length();
    }

    private PasswordParams getPasswordParams(String argument) {
        int passwordLength;
        String specialSymbols;

        if (argument == null || argument.isEmpty() || argument.isBlank()) {
            return new PasswordParams(DEFAULT_PASSWORD_LENGTH, SYMBOLS_SOURCE);
        }

        int indexOfSpace = argument.indexOf(" ");
        if (indexOfSpace < 0) {
            try {
                passwordLength = Integer.parseInt(argument);
            } catch (Exception e) {
                for (int i = 0; i < argument.length(); i++) {
                    if (SPECIAL_SYMBOLS.indexOf(argument.charAt(i)) == -1) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                return new PasswordParams(DEFAULT_PASSWORD_LENGTH, UPPER_LATIN + LOWER_LATIN + DIGITS + argument);
            }

            checkPasswordLength(passwordLength);
            return new PasswordParams(passwordLength, SYMBOLS_SOURCE);
        } else {
            try {
                passwordLength = Integer.parseInt(argument.substring(0, indexOfSpace));
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            specialSymbols = argument.substring(indexOfSpace + 1);
            for (int i = 0; i < specialSymbols.length(); i++) {
                if (SPECIAL_SYMBOLS.indexOf(specialSymbols.charAt(i)) == -1) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            }

            checkPasswordLength(passwordLength);
            return new PasswordParams(passwordLength, UPPER_LATIN + LOWER_LATIN + DIGITS + specialSymbols);
        }

    }

    private void checkPasswordLength(int length) {
        if (length < 4 || length > 4096) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String generatePassword(PasswordParams passwordParams) {
        String symbolsSource = passwordParams.getSymbolsSource();
        int sourceLength = symbolsSource.length();

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < passwordParams.getLength(); i++) {
            Integer position = MathUtils.getRandomInRange(0, sourceLength);
            buf.append(symbolsSource.charAt(position));
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

    @Value
    private static class PasswordParams {
        int length;
        String symbolsSource;
    }

}
