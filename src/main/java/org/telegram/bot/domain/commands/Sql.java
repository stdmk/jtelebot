package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

import static org.telegram.bot.utils.ExceptionUtils.getInitialExceptionCauseText;

@Component
@AllArgsConstructor
public class Sql implements CommandParent<SendMessage> {

    @PersistenceContext
    EntityManager entityManager;

    private final ApplicationContext context;
    private final SpeechService speechService;

    @Override
    @Transactional
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String responseText;
        String textMessage = cutCommandInText(message.getText());
        if (textMessage == null || textMessage.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (textMessage.toLowerCase().startsWith("select")) {
            try {
                StringBuilder buf = new StringBuilder();
                List<?> resultList = entityManager.createNativeQuery(textMessage).getResultList();
                if (resultList.isEmpty()) {
                    buf.append("Вернулся пустой ответ");
                } else {
                    try {
                        resultList.forEach(results -> {
                            buf.append("[");
                            Arrays.stream((Object[]) results).forEach(result -> buf.append(result.toString()).append(", "));
                            buf.append("]\n");
                        });
                    } catch (Exception e) {
                        buf.append("[");
                        resultList.forEach(result -> buf.append(result.toString()).append(", "));
                        buf.append("]");
                    }
                }
                responseText = buf.toString();
            } catch (Exception e) {
                responseText = getInitialExceptionCauseText(e);
            }
        }
        else if (textMessage.startsWith("update") || textMessage.startsWith("insert") || textMessage.startsWith("delete")) {
            int updated;
            try {
                updated = entityManager.createNativeQuery(textMessage).executeUpdate();
                responseText = "Успешно. Обновлено строк: " + updated;
            } catch (Exception e) {
                //because an UnexpectedRollbackException is thrown and prevents the message from being sent
                Bot bot = (Bot) context.getBean("bot");
                responseText = "Ошибка: " + getInitialExceptionCauseText(e);
                try {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(message.getChatId().toString());
                    sendMessage.setReplyToMessageId(message.getMessageId());
                    sendMessage.enableMarkdown(true);
                    sendMessage.setText("`" + responseText + "`");

                    bot.execute(sendMessage);
                } catch (TelegramApiException et) {
                    et.printStackTrace();
                }
                throw new BotException("Ошибка: " + getInitialExceptionCauseText(e));
            }
        }
        else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText("`" + responseText + "`");

        return sendMessage;
    }
}
