package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Calculator implements Command {

    private static final String MATH_JS_URL = "http://api.mathjs.org/v4/?expr=";

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate defaultRestTemplate;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String responseText;

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            log.debug("Empty request. Enabling command waiting");
            responseText = "${command.calculator.commandwaitingstart}";
        } else {
            commandArgument = commandArgument.replace(",", ".");
            log.debug("Request to calculate {}", commandArgument);

            JSONObject expressionData = new JSONObject();
            expressionData.put("expr", commandArgument);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> httpRequest = new HttpEntity<>(expressionData.toString(), headers);

            String result = "unavailable";
            try {
                ResponseEntity<String> response = defaultRestTemplate.postForEntity(MATH_JS_URL, httpRequest, String.class);
                if (response.getBody() == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
                }

                result = new JSONObject(response.getBody()).getString("result");
                responseText = "`" + new BigDecimal(result).toPlainString() + "`";
            } catch (HttpClientErrorException hce) {
                log.error("Error from api:", hce);
                responseText = new JSONObject(hce.getResponseBodyAsString()).getString("error");
            } catch (JSONException e) {
                botStats.incrementErrors(request, e, "service access error");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            } catch (NumberFormatException e) {
                responseText = "`" + result + "`";
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }
}
