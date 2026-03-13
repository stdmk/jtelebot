package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.WebTokenService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebToken implements Command {

    private final WebTokenService webTokenService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String token = webTokenService.createOrUpdateToken(message.getUser().getUserId());
        String responseText = "Ваш токен для входа в web-интерфейс:\n<pre><code>" + token + "</code></pre>";

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }
}
