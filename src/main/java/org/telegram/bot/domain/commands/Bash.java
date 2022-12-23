package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.charset.Charset;

import static org.telegram.bot.utils.TextUtils.cutMarkdownSymbolsInText;

@Component
@RequiredArgsConstructor
@Slf4j
public class Bash implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    private final static String BASHORG_URL = "http://bashorg.org";

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
        String BASH_RANDOM_QUOT_URL = BASHORG_URL + "/casual";

        String quot = getBashOrgRawData(BASH_RANDOM_QUOT_URL);
        checkForError(quot);

        String quoteNumber = quot.substring(quot.indexOf("<a href=\"/quote/") + 16);
        quoteNumber = quoteNumber.substring(0, quoteNumber.indexOf("\">"));
        quoteNumber = quoteNumber.replaceAll("/bayan", "");

        String date = quot.substring(quot.indexOf("</a>,-->") + 9);
        date = date.substring(0, date.indexOf("<a href"));

        quot = quot.substring(quot.indexOf("<div>") + 5);
        quot = quot.substring(0, quot.indexOf("</div>"));

        return buildResultMessage(quot, quoteNumber, date);
    }

    /**
     * Getting quot from Bash.org by number.
     *
     * @param quotNumber number of quot.
     * @return raw text of quot.
     */
    private String getDefineQuot(String quotNumber) {
        String BASH_DEFINITE_QUOT_URL = BASHORG_URL + "/quote";

        String quot = getBashOrgRawData(BASH_DEFINITE_QUOT_URL + "/" + quotNumber);
        checkForError(quot);

        String date = quot.substring(quot.indexOf("| добавлено: ") + 18);
        date = date.substring(date.indexOf("</a> ") + 5, date.indexOf("</div>") - 1);

        quot = quot.substring(quot.indexOf("<div class=\"quote\">") + 19);
        quot = quot.substring(0, quot.indexOf("</div>"));

        return buildResultMessage(quot, quotNumber, date);
    }

    private String getBashOrgRawData(String url) {
        try {
            return networkUtils.readStringFromURL(url, Charset.forName("windows-1251"));
        } catch (IOException e) {
            log.error("Error receiving quot", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }
    }

    private void checkForError(String data) {
        final String errorText = "не имеют доступа для просмотра статей из данного раздела";
        if (data.indexOf(errorText) > 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }
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

        return "[Цитата #" + quotNumber + "](" + BASHORG_URL + "/quote/" + quotNumber + ")\n" + "*" + date + "*\n" + quot;
    }
}
