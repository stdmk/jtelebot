package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.cutCommandInText;

@Component
@AllArgsConstructor
public class Sql extends CommandParent<SendMessage> {

    @PersistenceContext
    EntityManager entityManager;

    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        String responseText;
        String textMessage = cutCommandInText(update.getMessage().getText());
        if (textMessage == null || textMessage.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        try {
            StringBuilder buf = new StringBuilder();
            List resultList = entityManager.createNativeQuery(textMessage).getResultList();
            try {
                ((List<Object[]>) resultList).forEach(results -> {
                    buf.append("[");
                    Arrays.stream(results).forEach(result -> buf.append(result.toString()).append(", "));
                    buf.append("]\n");
                });
            } catch (Exception e) {
                buf.append("[");
                resultList.forEach(result -> buf.append(result.toString()).append(", "));
                buf.append("]");
            }
            responseText = buf.toString();
        } catch (Exception e) {
            while (e.getCause() != null) {
                e = (Exception) e.getCause();
            }
            e.printStackTrace();
            responseText = e.getLocalizedMessage();
        }

        return new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText("`" + responseText + "`");
    }
}
