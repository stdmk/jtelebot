package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;

import static org.telegram.bot.utils.TextUtils.cutMarkdownSymbolsInText;

@Component
@RequiredArgsConstructor
@Slf4j
public class Bash implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String quot;

        if (textMessage == null) {
            log.debug("Request to get random bash quot");
            quot = getRandomQuot();
        } else {
            log.debug("Request to get bash quot by number {}", textMessage);
            try {
                Integer.parseInt(textMessage);
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            quot = getDefineQuot(textMessage);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(quot);

        return sendMessage;
    }

    /**
     * Getting random quot from Bash.org.
     *
     * @return raw text of quot.
     */
    private String getRandomQuot() {
        String BASH_RANDOM_QUOT_URL = "https://bash.im/forweb/?u";
        String quot;

        try {
            quot = networkUtils.readStringFromURL(BASH_RANDOM_QUOT_URL);
        } catch (IOException e) {
            log.error("Error receiving quot", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        String quoteNumber = quot.substring(quot.indexOf("href=\"/quote/") + 13);
        quoteNumber = quoteNumber.substring(0, quoteNumber.indexOf("\">"));

        String date = quot.substring(quot.indexOf("'span style=\"padding: 15px 15px 12px\">") + 38);
        date = date.substring(0, date.indexOf("<' + '/span>"));

        quot = quot.substring(quot.indexOf("color: #21201e\">") + 16);
        quot = quot.substring(0, quot.indexOf("<' + '/div>"));

        return buildResultMessage(quot, quoteNumber, date);
    }

    /**
     * Getting quot from Bash.org by number.
     *
     * @param quotNumber number of quot.
     * @return raw text of quot.
     */
    private String getDefineQuot(String quotNumber) {
        String BASH_DEFINITE_QUOT_URL = "https://bash.im/quote/";
        String quot;

        try {
            quot = networkUtils.readStringFromURL(BASH_DEFINITE_QUOT_URL + quotNumber);
        } catch (IOException e) {
            log.error("Error receiving quot", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        String date = quot.substring(quot.indexOf("<div class=\"quote__header_date\">") + 41);
        date = date.substring(0, date.indexOf("</div>") - 7);

        quot = quot.substring(quot.indexOf("<div class=\"quote__body\">") + 32);
        quot = quot.substring(0, quot.indexOf("</div>"));

        return buildResultMessage(quot, quotNumber, date);
    }

    /**
     * Formatting raw text of Bash.org quot.
     *
     * @param quot raw text of quot.
     * @param quotNumber number of quot.
     * @param date date of quot.
     * @return formatted text of quot.
     */
    private String buildResultMessage(String quot, String quotNumber, String date) {
        quot = quot.replace("&quot;", "_");
        quot = quot.replace("<br>", "\n");
        quot = quot.replace("<br />", "\n");
        quot = quot.replace("<' + 'br>", "\n");
        quot = quot.replace("&lt;", "<").replace("&gt;", ">");
        quot = cutMarkdownSymbolsInText(quot);

        return "[Цитата #" + quotNumber + "](http://bash.im/quote/" + quotNumber + ")\n" + "_" + date + "_\n" + quot;
    }
}
