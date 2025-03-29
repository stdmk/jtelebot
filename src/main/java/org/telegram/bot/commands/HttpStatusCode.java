package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.util.List;

@RequiredArgsConstructor
@Component
public class HttpStatusCode implements Command {

    private final SpeechService speechService;
    private final LanguageResolver languageResolver;
    private final Environment environment;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        if (message.hasCommandArgument()) {
            String commandArgument = message.getCommandArgument();
            if (!TextUtils.isThatPositiveInteger(commandArgument)) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            String lang = languageResolver.getChatLanguageCode(message, message.getUser());
            String responseText = getHttpStatusCodeDescription(commandArgument, lang);

            return returnResponse(new TextResponse(message)
                    .setText(responseText)
                    .setResponseSettings(FormattingStyle.HTML));
        }

        return returnResponse();
    }

    private String getHttpStatusCodeDescription(String code, String lang) {
        String caption = environment.getProperty(lang + ".http.code." + code);
        String description = environment.getProperty(lang + ".http.status." + code);

        if (caption == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return "<b>" + code + " " + caption + "</b>\n" + description;
    }

}
