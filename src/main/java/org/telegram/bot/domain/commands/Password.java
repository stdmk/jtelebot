package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Password implements CommandParent<SendMessage> {

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        bot.sendTyping(update);
        int symbolsCount = 8;
        String text = getTextMessage(update);

        if (text != null) {
            try {
                symbolsCount = Integer.parseInt(getTextMessage(update));
            } catch (NumberFormatException e) {
                speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
            }
        }

        if (symbolsCount < 1 || symbolsCount > 1024) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        log.debug("Request to generate password with {} symbols", symbolsCount);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText("`" + new RandomString(symbolsCount).nextString() + "`");
        sendMessage.setReplyToMessageId(update.getMessage().getMessageId());
        sendMessage.enableMarkdown(true);

        return sendMessage;
    }
}
