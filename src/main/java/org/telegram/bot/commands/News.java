package org.telegram.bot.commands;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class News implements Command<PartialBotApiMethod<?>> {

    private static final int DEFAULT_NEWS_LIMIT = 10;
    private static final String WORD_PATTERN_TEMPLATE = "\\b(?:%s)\\b";

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
        Integer receivedMessageId = message.getMessageId();
        if (textMessage == null) {
            responseText = getLastNewsForChat(chat);
        } else if (textMessage.startsWith("_")) {
            NewsMessage newsMessage = getNewsById(textMessage.substring(1));

            responseText = buildFullNewsMessageText(newsMessage);
            Integer messageId = newsMessage.getMessageId();
            if (messageId == null) {
                messageId = receivedMessageId;
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
                    String potentialCount = textMessage.substring(spaceIndex + 1);
                    if (isThatInteger(potentialCount)) {
                        count = Integer.parseInt(potentialCount);
                    } else {
                        count = DEFAULT_NEWS_LIMIT;
                    }
                } else {
                    url = textMessage;
                    count = DEFAULT_NEWS_LIMIT;
                }
            }

            if (isThatUrl(url)) {
                checkNewsCount(count);
                responseText = getAllNews(url, count, receivedMessageId);
            } else {
                responseText = searchForNews(textMessage, receivedMessageId);
            }
        }

        if (responseText.isEmpty()) {
            responseText = speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(receivedMessageId);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private void checkNewsCount(int newsCount) {
        if (newsCount < 1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private String searchForNews(String text, Integer messageId) {
        String words = Arrays.stream(text.split(" ")).collect(Collectors.joining("|", "", ""));
        Pattern wordsPattern = Pattern.compile(String.format(WORD_PATTERN_TEMPLATE, words), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

        return newsSourceService.getAll()
                .stream()
                .map(newsSource -> searchNewsInNewsSource(newsSource, wordsPattern, messageId))
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());
    }

    private String searchNewsInNewsSource(NewsSource newsSource, Pattern pattern, Integer messageId) {
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
                .filter(syndEntry -> entryMatchesPattern(syndEntry, pattern))
                .map(syndEntry -> buildNewsMessageFromSyndEntry(syndEntry, messageId))
                .collect(Collectors.toList());

        return newsMessageService.save(newsMessageList)
                .stream()
                .map(newsMessage -> buildShortNewsMessageText(newsMessage, newsSource.getName()))
                .collect(Collectors.joining());
    }

    private boolean entryMatchesPattern(SyndEntry syndEntry, Pattern pattern) {
        String title = syndEntry.getTitle();
        String description = Optional.ofNullable(syndEntry.getDescription()).map(SyndContent::getValue).orElse(null);

        return (title != null && pattern.matcher(title).find())
                || (description != null && pattern.matcher(description).find());
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
    private String getAllNews(String url, int newsCount, Integer messageId) {
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
            return buildListOfNewsMessageText(syndFeed.getEntries().stream().limit(newsCount).collect(Collectors.toList()), messageId);
        }

        return buildFullNewsMessageText(syndFeed.getEntries().get(0), messageId);
    }

    /**
     * Creating formatted list of news from feeds.
     *
     * @param entries feeds of news.
     * @return formatted news.
     */
    private String buildListOfNewsMessageText(List<SyndEntry> entries, Integer messageId) {
        List<NewsMessage> newsMessages = entries
                .stream()
                .map(syndEntry -> buildNewsMessageFromSyndEntry(syndEntry, messageId))
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

    private String buildFullNewsMessageText(SyndEntry syndEntry, Integer messageId) {
        return buildFullNewsMessageText(buildNewsMessageFromSyndEntry(syndEntry, messageId));
    }

    public NewsMessage buildNewsMessageFromSyndEntry(SyndEntry syndEntry) {
        return buildNewsMessageFromSyndEntry(syndEntry, null);
    }

    private NewsMessage buildNewsMessageFromSyndEntry(SyndEntry syndEntry, Integer messageId) {
        String title = reduceSpaces(cutHtmlTags(syndEntry.getTitle()));
        if (title.length() > 255) {
            int i = title.indexOf(".");
            if (i < 0 || i > 255) {
                title = title.substring(0, 50) + "...";
            } else {
                title = title.substring(0, i);
            }
        }

        String descHash;
        String description;
        if (syndEntry.getDescription() == null) {
            description = "";
            descHash = DigestUtils.sha256Hex(title);
        } else {
            description = reduceSpaces(cutHtmlTags(syndEntry.getDescription().getValue()));
            if (description.length() > 768) {
                description = description.substring(0, 767) + "...";
            }
            descHash = DigestUtils.sha256Hex(description);
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
                .setAttachUrl(getAttachUrl(syndEntry))
                .setMessageId(messageId)
                .setDescHash(descHash);
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
