package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;

import static org.telegram.bot.utils.TextUtils.cutMarkdownSymbolsInText;
import static org.telegram.bot.utils.NetworkUtils.readStringFromURL;

@Component
@AllArgsConstructor
public class Bash implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Bash.class);

    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
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

        if (quot == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(quot);

        return sendMessage;
    }

    private String getRandomQuot() {
        String BASH_RANDOM_QUOT_URL = "https://bash.im/forweb/?u";
        String quot;

        try {
            quot = readStringFromURL(BASH_RANDOM_QUOT_URL);
        } catch (IOException e) {
            return null;
        }
        if (quot == null) {
            return null;
        }

        String quoteNumber = quot.substring(quot.indexOf("href=\"/quote/") + 13);
        quoteNumber = quoteNumber.substring(0, quoteNumber.indexOf("\">"));

        String date = quot.substring(quot.indexOf("'span style=\"padding: 15px 15px 12px\">") + 38);
        date = date.substring(0, date.indexOf("<' + '/span>"));

        quot = quot.substring(quot.indexOf("color: #21201e\">") + 16);
        quot = quot.substring(0, quot.indexOf("<' + '/div>"));
        quot = formatBashQuot(quot);

        return buildResultMessage(quot, quoteNumber, date);
    }

    private String getDefineQuot(String quotNumber) {
        String BASH_DEFINITE_QUOT_URL = "https://bash.im/quote/";
        String quot;

        try {
            quot = readStringFromURL(BASH_DEFINITE_QUOT_URL + quotNumber);
        } catch (IOException e) {
            return null;
        }
        if (quot == null) {
            return null;
        }

        String date = quot.substring(quot.indexOf("<div class=\"quote__header_date\">") + 41);
        date = date.substring(0, date.indexOf("</div>") - 7);

        quot = quot.substring(quot.indexOf("<div class=\"quote__body\">") + 32);
        quot = quot.substring(0, quot.indexOf("</div>"));
        quot = formatBashQuot(quot);

        return buildResultMessage(quot, quotNumber, date);
    }

    private String formatBashQuot(String quot) {
        quot = quot.replace("&quot;", "_");
        quot = quot.replace("<br>", "\n");
        quot = quot.replace("<br />", "\n");
        quot = quot.replace("<' + 'br>", "\n");

        return quot;
    }

    private String buildResultMessage(String quot, String quotNumber, String date) {
        quot = cutMarkdownSymbolsInText(quot);
        quot = quot.replace("&lt;", "<").replace("&gt;", ">");
        return "[Цитата #" + quotNumber + "](http://bash.im/quote/" + quotNumber + ")\n" + "_" + date + "_\n" + quot;
    }
}
