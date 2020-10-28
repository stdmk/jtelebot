package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.NewsSourceService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;

@Component
@AllArgsConstructor
public class NewsSource implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(NewsSource.class);

    private final NewsSourceService newsSourceService;
    private final SpeechService speechService;

    private static final List<String> PARAMS = Arrays.asList("добавить", "удалить");

    @Override
    public SendMessage parse(Update update) throws BotException {
        String textMessage = cutCommandInText(update.getMessage().getText());
        String responseText;

        if (textMessage == null) {
            log.debug("Request to list all news sources");
            final StringBuilder buf = new StringBuilder();
            buf.append("*Список новостных источников:*\n");
            newsSourceService.getAll()
                    .forEach(newsSource -> buf.append(newsSource.getId()).append(") ")
                                            .append("[").append(newsSource.getName()).append("](")
                                            .append(newsSource.getUrl()).append(")\n"));
            responseText = buf.toString();
        } else {
            if (textMessage.startsWith(PARAMS.get(0))) {
                log.debug("Request to add new news resource");
                String params = textMessage.substring(9);
                int i = params.indexOf(" ");
                if (i < 0) {
                    throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
                }

                String name;
                String url;
                if (!params.startsWith("http") && !params.substring(i + 1).startsWith("http")) {
                    throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
                }
                if (params.startsWith("http")) {
                    url = params.substring(0, i);
                    name = params.substring(i + 1);
                } else {
                    url = params.substring(i + 1);
                    name = params.substring(0, i);
                }

                org.telegram.bot.domain.entities.NewsSource newsSource = newsSourceService.get(name, url);
                if (newsSource != null) {
                    throw new BotException("Такой источник уже существует: " + newsSource.getName() + " - " + newsSource.getUrl());
                }

                newsSource = new org.telegram.bot.domain.entities.NewsSource();
                newsSource.setName(name);
                newsSource.setUrl(url);
                newsSourceService.save(newsSource);
                responseText = speechService.getRandomMessageByTag("saved");
            }
            else if (textMessage.startsWith(PARAMS.get(1))) {
                log.debug("Request to delete news resource");
                String params = textMessage.substring(8);
                try {
                    Long newsSourceId = Long.parseLong(params);
                    if (newsSourceService.remove(newsSourceId)) {
                        responseText = speechService.getRandomMessageByTag("saved");
                    } else {
                        responseText = speechService.getRandomMessageByTag("wrongInput");
                    }
                } catch (Exception e) {
                    if (newsSourceService.remove(params)) {
                        responseText = speechService.getRandomMessageByTag("saved");
                    } else {
                        responseText = speechService.getRandomMessageByTag("wrongInput");
                    }
                }
            }
            else {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }
        }

        return new SendMessage().setReplyToMessageId(update.getMessage().getMessageId())
                .setChatId(update.getMessage().getChatId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText(responseText);
    }
}
