package org.telegram.bot.domain.commands;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.NetworkUtils.getFileFromUrl;
import static org.telegram.bot.utils.NetworkUtils.getRssFeedFromUrl;

@Component
@AllArgsConstructor
public class News implements CommandParent<PartialBotApiMethod<?>> {

    private final Logger log = LoggerFactory.getLogger(News.class);

    private final NewsService newsService;
    private final NewsMessageService newsMessageService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        Chat chat = chatService.get(message.getChatId());
        String responseText;

        if (textMessage == null) {
            log.debug("Request to list all news sources for chat {}", chat.getChatId());
            final StringBuilder buf = new StringBuilder();
            buf.append("<b>Список новостных источников:</b>\n");
            newsService.getAll(chat)
                    .forEach(news -> buf.append(news.getId()).append(" - ")
                                            .append("<a href=\"").append(news.getNewsSource().getUrl()).append("\">")
                                            .append(news.getName()).append("</a>\n"));
            responseText = buf.toString();
        } else if (textMessage.startsWith("_")) {
            long newsId;
            try {
                newsId = Long.parseLong(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }

            NewsMessage newsMessage = newsMessageService.get(newsId);
            if (newsMessage == null) {
                throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
            }

            responseText = newsMessageService.buildFullNewsMessageText(newsMessage);
            Integer messageId = newsMessage.getMessageId();
            if (messageId == null) {
                messageId = message.getMessageId();
            }
            InputStream image;
            try {
                image = getFileFromUrl(newsMessage.getAttachUrl());
            } catch (Exception e) {
                return new SendMessage()
                        .setChatId(message.getChatId())
                        .setReplyToMessageId(messageId)
                        .setParseMode(ParseModes.HTML.getValue())
                        .disableWebPagePreview()
                        .setText(responseText);
            }

            return new SendPhoto()
                    .setPhoto("news", image)
                    .setCaption(responseText)
                    .setParseMode(ParseModes.HTML.getValue())
                    .setReplyToMessageId(messageId)
                    .setChatId(message.getChatId());
        } else if (textMessage.startsWith("http")) {
            log.debug("Request to get news by url {}", textMessage);
            responseText = getAllNews(textMessage);
        } else {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        return new SendMessage()
                .setReplyToMessageId(message.getMessageId())
                .setChatId(message.getChatId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText(responseText);
    }

    private String getAllNews(String url) {
        SyndFeed feed = getRssFeedFromUrl(url);
        if (feed == null) {
            return speechService.getRandomMessageByTag("noResponse");
        }

        return buildListOfNewsMessageText(feed.getEntries());
    }

    private String buildListOfNewsMessageText(List<SyndEntry> entries) {
        List<NewsMessage> newsMessages = entries
                .stream()
                .map(newsMessageService::buildNewsMessageFromSyndEntry)
                .collect(Collectors.toList());

        newsMessages = newsMessageService.save(newsMessages);

        StringBuilder buf = new StringBuilder();
        newsMessages.forEach(newsMessage -> buf.append(newsMessageService.buildShortNewsMessageText(newsMessage)));

        return buf.toString();
    }
}
