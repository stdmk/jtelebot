package org.telegram.bot.commands;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.NewsMessageService;
import org.telegram.bot.services.NewsService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.cutHtmlTags;
import static org.telegram.bot.utils.TextUtils.reduceSpaces;

@Component
@RequiredArgsConstructor
@Slf4j
public class News implements Command<PartialBotApiMethod<?>> {

    private static final int DEFAULT_NEWS_LIMIT = 10;

    private final Bot bot;
    private final NewsService newsService;
    private final NewsMessageService newsMessageService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        Chat chat = new Chat().setChatId(message.getChatId());

        String responseText;
        if (textMessage == null) {
            responseText = getLastNewsForChat(chat);
        } else if (textMessage.startsWith("_")) {
            NewsMessage newsMessage = getNewsById(textMessage.substring(1));

            responseText = buildFullNewsMessageText(newsMessage);
            Integer messageId = newsMessage.getMessageId();
            if (messageId == null) {
                messageId = message.getMessageId();
            }
            if (newsMessage.getAttachUrl() == null) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setReplyToMessageId(messageId);
                sendMessage.enableHtml(true);
                sendMessage.disableWebPagePreview();
                sendMessage.setText(responseText);

                return sendMessage;
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(newsMessage.getAttachUrl()));
            sendPhoto.setCaption(responseText);
            sendPhoto.setParseMode("HTML");
            sendPhoto.setReplyToMessageId(messageId);
            sendPhoto.setChatId(message.getChatId().toString());

            return sendPhoto;
        } else {
            log.debug("Request to get news from {}", textMessage);

            org.telegram.bot.domain.entities.News news = newsService.get(chat, textMessage);

            String url;
            int count;
            if (news != null) {
                url = news.getNewsSource().getUrl();
                count = DEFAULT_NEWS_LIMIT;
            } else {
                int spaceIndex = textMessage.indexOf(" ");
                if (spaceIndex > 0) {
                    url = textMessage.substring(0, spaceIndex);
                    try {
                        count = Integer.parseInt(textMessage.substring(spaceIndex + 1));
                    } catch (NumberFormatException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                } else {
                    url = textMessage;
                    count = DEFAULT_NEWS_LIMIT;
                }
            }

            checkUrl(url);
            checkNewsCount(count);
            responseText = getAllNews(url, count);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private void checkUrl(String text) {
        try {
             new URL(text);
        } catch (MalformedURLException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private void checkNewsCount(int newsCount) {
        if (newsCount < 1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String getLastNewsForChat(Chat chat) {
        log.debug("Request to get last news for chat {}", chat.getChatId());
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${command.news.lastnews}:</b>\n\n");
        newsService.getAll(chat)
                .forEach(news -> {
                    NewsMessage newsMessage = news.getNewsSource().getNewsMessage();
                    if (newsMessage != null) {
                        buf.append(buildShortNewsMessageText(news.getNewsSource().getNewsMessage(), news.getName()));
                    }
                });
        return buf.toString();
    }

    private NewsMessage getNewsById(String id) {
        long newsId;
        try {
            newsId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        log.debug("Request to get details of news by id {}", newsId);
        NewsMessage newsMessage = newsMessageService.get(newsId);
        if (newsMessage == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return newsMessage;
    }

    /**
     * Getting formatted list of news from url.
     *
     * @param url url of rss resource.
     * @param newsCount requested news count.
     * @return formatted news.
     */
    private String getAllNews(String url, int newsCount) {
        SyndFeed syndFeed;
        try {
            syndFeed = networkUtils.getRssFeedFromUrl(url);
        } catch (FeedException e) {
            log.debug("Failed to parse news from url: {}: {}", url, e.getMessage());
            return speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        } catch (MalformedURLException e) {
            log.debug("Malformed URL: {}", url);
            return speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
        } catch (IOException e) {
            log.debug("Failed to connect to url: {}: {}", url, e.getMessage());
            return speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR);
        }

        if (newsCount > 1) {
            return buildListOfNewsMessageText(syndFeed.getEntries().stream().limit(newsCount).collect(Collectors.toList()));
        }

        return buildFullNewsMessageText(buildNewsMessageFromSyndEntry(syndFeed.getEntries().get(0)));
    }

    /**
     * Creating formatted list of news from feeds.
     *
     * @param entries feeds of news.
     * @return formatted news.
     */
    private String buildListOfNewsMessageText(List<SyndEntry> entries) {
        List<NewsMessage> newsMessages = entries
                .stream()
                .map(this::buildNewsMessageFromSyndEntry)
                .collect(Collectors.toList());

        newsMessages = newsMessageService.save(newsMessages);

        StringBuilder buf = new StringBuilder();
        newsMessages.forEach(newsMessage -> buf.append(buildShortNewsMessageText(newsMessage)));

        return buf.toString();
    }

    public String buildShortNewsMessageText(NewsMessage newsMessage, String sourceName) {
        return "<b>" + newsMessage.getTitle() + "</b> <a href='" + newsMessage.getLink() + "'>(" + sourceName + ")</a>\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    private String buildShortNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    public NewsMessage buildNewsMessageFromSyndEntry(SyndEntry syndEntry) {
        String title = reduceSpaces(cutHtmlTags(syndEntry.getTitle()));
        if (title.length() > 255) {
            int i = title.indexOf(".");
            if (i < 0 || i > 255) {
                title = title.substring(0, 50) + "...";
            } else {
                title = title.substring(0, i);
            }
        }

        String description;
        if (syndEntry.getDescription() == null) {
            description = "";
        } else {
            description = reduceSpaces(cutHtmlTags(syndEntry.getDescription().getValue()));
            if (description.length() > 768) {
                description = description.substring(0, 767) + "...";
            }
        }

        Date publishedDate = syndEntry.getPublishedDate();
        if (publishedDate == null) {
            publishedDate = syndEntry.getUpdatedDate();
            if (publishedDate == null) {
                publishedDate = Date.from(Instant.now());
            }
        }

        return new NewsMessage()
                .setLink(syndEntry.getLink())
                .setTitle(title)
                .setDescription(description)
                .setPubDate(publishedDate)
                .setAttachUrl(getAttachUrl(syndEntry));
    }

    private String buildFullNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n" +
                "<i>" + formatDate(newsMessage.getPubDate()) + "</i>\n" +
                newsMessage.getDescription() +
                "\n<a href=\"" + newsMessage.getLink() + "\">Читать полностью</a>";
    }

    private String getAttachUrl(SyndEntry syndEntry) {
        if (!syndEntry.getEnclosures().isEmpty()) {
            return syndEntry.getEnclosures().get(0).getUrl();
        }

        Optional<String> optionalDesc = Optional.of(syndEntry).map(SyndEntry::getDescription).map(SyndContent::getValue);
        if (optionalDesc.isPresent()) {
            String description = optionalDesc.get();
            int a = description.indexOf("<img");
            if (a >= 0) {
                String buf = description.substring(a);
                int b = buf.indexOf("/>");
                if (b < 0) {
                    b = buf.indexOf("/img>");
                    if (b < 0) {
                        b = buf.indexOf(">");
                        if (b < 0) {
                            return null;
                        }
                    }
                }
                String imageTag = buf.substring(4, b);

                a = imageTag.indexOf("src=");
                if (a < 0) {
                    return null;
                }
                buf = imageTag.substring(a + 5);
                b = buf.indexOf("\"");
                if (b < 0) {
                    return null;
                }

                return buf.substring(0, b);
            }
        }

        return null;
    }

}
