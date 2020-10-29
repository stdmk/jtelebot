package org.telegram.bot.domain.commands;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.NewsService;
import org.telegram.bot.services.NewsSourceService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.NetworkUtils.getImageFromUrl;
import static org.telegram.bot.utils.TextUtils.reduceSpaces;

@Component
@AllArgsConstructor
public class News implements CommandParent {

    private final Logger log = LoggerFactory.getLogger(News.class);

    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public PartialBotApiMethod parse(Update update) throws Exception {
        String textMessage = cutCommandInText(update.getMessage().getText());
        Chat chat = chatService.get(update.getMessage().getChatId());
        String responseText;

        if (textMessage == null) {
            log.debug("Request to list all last news for this chat: {}", chat.getChatId());
            responseText = "<b>Список последних новостей:\n</b>" + buildListOfNewsMessageText(getLastNewsForChat(chat));
        } else {
            if (textMessage.startsWith("_")) {
                long newsId;
                try {
                    newsId = Long.parseLong(textMessage.substring(1));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
                }

                org.telegram.bot.domain.entities.News news = newsService.get(newsId);
                if (news == null) {
                    throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
                }

                responseText = buildFullNewsMessageText(news);
                InputStream image;
                try {
                    image = getImageFromUrl(news.getAttachUrl());
                } catch (Exception e) {
                    return new SendMessage()
                            .setChatId(update.getMessage().getChatId())
                            .setReplyToMessageId(update.getMessage().getMessageId())
                            .setParseMode(ParseModes.HTML.getValue())
                            .setText(responseText);
                }

                return new SendPhoto()
                        .setPhoto("news", image)
                        .setCaption(responseText)
                        .setParseMode(ParseModes.HTML.getValue())
                        .setReplyToMessageId(update.getMessage().getMessageId())
                        .setChatId(update.getMessage().getChatId());
            }
            if (textMessage.startsWith("http")) {
                log.debug("Request to get news by url {}", textMessage);
                responseText = getAllNews(textMessage);
            } else {
                NewsSource newsSource = newsSourceService.get(chat, textMessage);
                if (newsSource == null) {
                    responseText = speechService.getRandomMessageByTag("wrongInput");
                } else {
                    log.debug("Request to get news by NewsSource {}", newsSource);
                    responseText = getAllNews(newsSource.getUrl());
                }
            }
        }

        return new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText(responseText);
    }

    private List<org.telegram.bot.domain.entities.News> getLastNewsForChat(Chat chat) {
        return new ArrayList<>();
    }

    private String getAllNews(String url) {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try {
            feed = input.build(new XmlReader(new URL(url)));
        } catch (FeedException | IOException e) {
            log.error(e.getLocalizedMessage());
        }

        if (feed == null) {
            return speechService.getRandomMessageByTag("noResponse");
        }

        List<org.telegram.bot.domain.entities.News> newsList = newsService.save(feed.getEntries()
                .stream()
                .map(syndEntry -> {
                    org.telegram.bot.domain.entities.News news = new org.telegram.bot.domain.entities.News();
                    news.setTitle(syndEntry.getTitle());
                    news.setLink(syndEntry.getLink());
                    news.setDescription(reduceSpaces(syndEntry.getDescription().getValue()));
                    news.setPubDate(syndEntry.getPublishedDate());
                    news.setAttachUrl(syndEntry.getEnclosures().get(0).getUrl());

                    return news;
                })
                .collect(Collectors.toList()));

        return buildListOfNewsMessageText(newsList);

    }

    private String buildListOfNewsMessageText(List<org.telegram.bot.domain.entities.News> newsList) {
        StringBuilder buf = new StringBuilder();
        newsList.forEach(news -> buf.append(buildShortNewsMessageText(news)));

        return buf.toString();
    }

    private String buildShortNewsMessageText(org.telegram.bot.domain.entities.News news) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return "<b>" + news.getTitle() + "</b>\n" + dateTimeFormat.format(news.getPubDate()) + " /news_" + news.getId() + "\n\n";
    }

    private String buildFullNewsMessageText(org.telegram.bot.domain.entities.News news) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return "<b>" + news.getTitle() + "</b>\n" +
                "<i>" + dateTimeFormat.format(news.getPubDate()) + "</i>\n" +
                news.getDescription() +
                "\n<a href=\"" + news.getLink() + "\">Читать полностью</a>";
    }
}
