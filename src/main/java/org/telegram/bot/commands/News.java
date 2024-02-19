package org.telegram.bot.commands;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.NewsMessageService;
import org.telegram.bot.services.NewsService;
import org.telegram.bot.services.NewsSourceService;
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
import java.time.Instant;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class News implements Command<PartialBotApiMethod<?>> {

    private static final int DEFAULT_NEWS_LIMIT = 10;

    private final Bot bot;
    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final NewsMessageService newsMessageService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;
    private final BotStats botStats;

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

            if (isThatUrl(url)) {
                checkNewsCount(count);
                responseText = getAllNews(url, count);
            } else {
                responseText = searchForNews(textMessage);
            }
        }

        if (responseText.isEmpty()) {
            responseText = speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private void checkNewsCount(int newsCount) {
        if (newsCount < 1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String searchForNews(String text) {
        Set<String> searchWords = Arrays.stream(text.split(" ")).collect(Collectors.toSet());
        return newsSourceService.getAll()
                .stream()
                .map(newsSource -> searchNewsInNewsSource(newsSource, searchWords))
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());
    }

    private String searchNewsInNewsSource(NewsSource newsSource, Set<String> searchWords) {
        String url = newsSource.getUrl();
        SyndFeed syndFeed;
        try {
            syndFeed = networkUtils.getRssFeedFromUrl(newsSource.getUrl());
        } catch (FeedException e) {
            log.error("Failed to parse news from url: {}: {}", url, e.getMessage());
            botStats.incrementErrors(url, e, "Failed to parse news from url");
            return null;
        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", url);
            botStats.incrementErrors(url, e, "Malformed URL");
            return null;
        } catch (IOException e) {
            log.error("Failed to connect to url: {}: {}", url, e.getMessage());
            botStats.incrementErrors(url, e, "Failed to connect to ur");
            return null;
        }


        List<NewsMessage> newsMessageList = syndFeed.getEntries()
                .stream()
                .filter(syndEntry -> entryHasAnyWords(syndEntry, searchWords))
                .map(this::buildNewsMessageFromSyndEntry)
                .collect(Collectors.toList());

        return newsMessageService.save(newsMessageList)
                .stream()
                .map(newsMessage -> buildShortNewsMessageText(newsMessage, newsSource.getName()))
                .collect(Collectors.joining("\n"));
    }

    private boolean entryHasAnyWords(SyndEntry syndEntry, Set<String> words) {
        String title = syndEntry.getTitle();
        String description = Optional.ofNullable(syndEntry.getDescription()).map(SyndContent::getValue).orElse(null);

        return (title != null && words.stream().anyMatch(word -> title.toLowerCase().contains(word)))
                || (description != null && words.stream().anyMatch(word -> description.toLowerCase().contains(word)));
    }

    private String getLastNewsForChat(Chat chat) {
        log.debug("Request to get last news for chat {}", chat.getChatId());
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${command.news.lastnews}:</b>\n\n");
        newsService.getAll(chat)
                .forEach(news -> {
                    NewsMessage newsMessage = news.getNewsSource().getNewsMessage();
                    if (newsMessage != null) {
                        buf.append(buildShortNewsMessageText(news.getNewsSource().getNewsMessage(), news.getNewsSource().getName()));
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

        return buildFullNewsMessageText(syndFeed.getEntries().get(0));
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

    private String buildFullNewsMessageText(SyndEntry syndEntry) {
        return buildFullNewsMessageText(buildNewsMessageFromSyndEntry(syndEntry));
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
