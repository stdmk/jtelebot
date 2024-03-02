package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.cutMarkdownSymbolsInText;

@Component
@RequiredArgsConstructor
@Slf4j
public class Bash implements Command<SendMessage> {

    private final Bot bot;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    private static final String BASHORG_URL = "https://xn--80abh7bk0c.xn--p1ai";
    private static final String BASH_RANDOM_QUOT_URL = BASHORG_URL + "/random";
    private static final String BASH_DEFINITE_QUOT_URL = BASHORG_URL + "/quote";

    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String quote;

        if (textMessage == null) {
            log.debug("Request to get random bash quote");
            quote = getRandomQuot();
        } else {
            log.debug("Request to get bash quote by number {}", textMessage);
            try {
                Integer.parseInt(textMessage);
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            quote = getDefineQuot(textMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(quote);

        return List.of(sendMessage);
    }

    /**
     * Getting random quote from Bash.org.
     *
     * @return raw text of quote.
     */
    private String getRandomQuot() {
        return buildResponse(getBashOrgRawData(BASH_RANDOM_QUOT_URL));
    }

    /**
     * Getting quote from Bash.org by number.
     *
     * @param quoteNumber number of quote.
     * @return raw text of quote.
     */
    private String getDefineQuot(String quoteNumber) {
        return buildResponse(getBashOrgRawData(BASH_DEFINITE_QUOT_URL + "/" + quoteNumber));
    }

    private String getBashOrgRawData(String url) {
        try {
            return networkUtils.readStringFromURL(url);
        } catch (IOException e) {
            log.error("Error receiving quote", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }
    }

    private String buildResponse(String quoteRaw) {
        String quoteNumber = getQuoteNumber(quoteRaw);
        if (quoteNumber.isBlank()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        String date = getQuoteDate(quoteRaw);
        String quote = getQuote(quoteRaw);

        return buildResponse(quote, quoteNumber, date);
    }

    private String getQuote(String rawData) {
        String buf = rawData.substring(rawData.indexOf("<div class=\"quote__body\">") + 25);
        return buf.substring(0, buf.indexOf("</div>")).trim();
    }

    private String getQuoteDate(String rawData) {
        String buf = rawData.substring(rawData.indexOf("<div class=\"quote__header_date\">") + 32);
        return buf.substring(0, buf.indexOf("</div>") - 1).trim();
    }

    private String getQuoteNumber(String rawData) {
        String buf = rawData.substring(rawData.indexOf("data-quote=\"") + 12);
        return buf.substring(0, buf.indexOf("\">"));
    }

    /**
     * Formatting raw text of Bash.org quote.
     *
     * @param quote raw text of quote.
     * @param quotNumber number of quote.
     * @param date date of quote.
     * @return formatted text of quote.
     */
    private String buildResponse(String quote, String quotNumber, String date) {
        quote = quote.replace("&quote;", "_");
        quote = quote.replace("<br>", "\n");
        quote = quote.replace("<br />", "\n");
        quote = quote.replace("<' + 'br>", "\n");
        quote = quote.replace("&lt;", "<").replace("&gt;", ">");
        quote = cutMarkdownSymbolsInText(quote);

        return "[Цитата #" + quotNumber + "](" + BASHORG_URL + "/quote/" + quotNumber + ")\n" + "*" + date + "*\n" + quote;
    }
}
