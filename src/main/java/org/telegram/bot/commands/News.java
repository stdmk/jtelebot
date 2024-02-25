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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class News implements Command<PartialBotApiMethod<?>> {

    private static final String WORD_PATTERN_TEMPLATE = "\\b(?:%s)\\b";
    private static final Pattern PARAMS_PATTERN = Pattern.compile(
            "(?:\"([^\"]*?)\"|(\\b\\w+\\b))(?:\\s|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private final Bot bot;
    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final NewsMessageService newsMessageService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;
    private final BotStats botStats;

    @Override
    public List<PartialBotApiMethod<?>> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        Chat chat = new Chat().setChatId(message.getChatId());

        List<String> responseTextList;
        Integer messageId = message.getMessageId();
        if (textMessage == null) {
            responseTextList = getLastNewsForChat(chat);
        } else if (textMessage.startsWith("_")) {
            NewsMessage newsMessage = getNewsById(textMessage.substring(1));

            String responseText = buildFullNewsMessageText(newsMessage);

            if (newsMessage.getAttachUrl() == null) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setReplyToMessageId(messageId);
                sendMessage.enableHtml(true);
                sendMessage.disableWebPagePreview();
                sendMessage.setText(responseText);

                return returnOneResult(sendMessage);
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(newsMessage.getAttachUrl()));
            sendPhoto.setCaption(responseText);
            sendPhoto.setParseMode("HTML");
            sendPhoto.setReplyToMessageId(messageId);
            sendPhoto.setChatId(message.getChatId().toString());

            return returnOneResult(sendPhoto);
        } else {
            log.debug("Request to get news from {}", textMessage);

            org.telegram.bot.domain.entities.News news = newsService.get(chat, textMessage);

            String url;
            Integer count = null;
            if (news != null) {
                url = news.getNewsSource().getUrl();
            } else {
                int spaceIndex = textMessage.indexOf(" ");
                if (spaceIndex > 0) {
                    url = textMessage.substring(0, spaceIndex);
                    String potentialCount = textMessage.substring(spaceIndex + 1);
                    if (isThatInteger(potentialCount)) {
                        count = Integer.parseInt(potentialCount);
                    }
                } else {
                    url = textMessage;
                }
            }

            if (isThatUrl(url)) {
                checkNewsCount(count);
                responseTextList = getAllNews(url, count);
            } else {
                responseTextList = searchForNews(textMessage);
            }
        }

        if (responseTextList.isEmpty()) {
            responseTextList = List.of(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return mapToSendMessages(responseTextList, message.getChatId(), messageId);
    }

    private void checkNewsCount(Integer newsCount) {
        if (newsCount == null) {
            return;
        }
        if (newsCount < 1) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private List<String> searchForNews(String text) {
        String words = splitParams(text).stream().collect(Collectors.joining("|", "", ""));
        Pattern wordsPattern = Pattern.compile(String.format(WORD_PATTERN_TEMPLATE, words), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

        return newsSourceService.getAll()
                .stream()
                .map(newsSource -> searchNewsInNewsSource(newsSource, wordsPattern))
                .flatMap(Collection::stream)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Set<String> splitParams(String text) {
        Set<String> result = new HashSet<>();

        Matcher matcher = PARAMS_PATTERN.matcher(text);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                result.add(matcher.group(1));
            } else {
                result.add(matcher.group(2));
            }
        }

        return result;
    }

    private List<String> searchNewsInNewsSource(NewsSource newsSource, Pattern pattern) {
        String url = newsSource.getUrl();
        SyndFeed syndFeed;
        try {
            syndFeed = networkUtils.getRssFeedFromUrl(newsSource.getUrl());
        } catch (FeedException e) {
            log.error("Failed to parse news from url: {}: {}", url, e.getMessage());
            botStats.incrementErrors(url, e, "Failed to parse news from url");
            return Collections.emptyList();
        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", url);
            botStats.incrementErrors(url, e, "Malformed URL");
            return Collections.emptyList();
        } catch (IOException e) {
            log.error("Failed to connect to url: {}: {}", url, e.getMessage());
            botStats.incrementErrors(url, e, "Failed to connect to ur");
            return Collections.emptyList();
        }

        List<NewsMessage> newsMessageList = syndFeed.getEntries()
                .stream()
                .filter(syndEntry -> entryMatchesPattern(syndEntry, pattern))
                .map(this::buildNewsMessageFromSyndEntry)
                .collect(Collectors.toList());

        return newsMessageService.save(newsMessageList)
                .stream()
                .map(newsMessage -> buildShortNewsMessageText(newsMessage, newsSource.getName()))
                .collect(Collectors.toList());
    }

    private boolean entryMatchesPattern(SyndEntry syndEntry, Pattern pattern) {
        String title = syndEntry.getTitle();
        String description = Optional.ofNullable(syndEntry.getDescription()).map(SyndContent::getValue).orElse(null);

        return (title != null && pattern.matcher(title).find())
                || (description != null && pattern.matcher(description).find());
    }

    private List<String> getLastNewsForChat(Chat chat) {
        log.debug("Request to get last news for chat {}", chat.getChatId());
        List<String> result = new ArrayList<>();
        result.add("<b>${command.news.lastnews}:</b>\n\n");

        result.addAll(newsService.getAll(chat)
                .stream()
                .map(news -> {
                    NewsMessage newsMessage = news.getNewsSource().getNewsMessage();
                    if (newsMessage != null) {
                        return buildShortNewsMessageText(news.getNewsSource().getNewsMessage(), news.getNewsSource().getName());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        return result;
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
    private List<String> getAllNews(String url, Integer newsCount) {
        SyndFeed syndFeed;
        try {
            syndFeed = networkUtils.getRssFeedFromUrl(url);
        } catch (FeedException e) {
            log.error("Failed to parse news from url: {}: {}", url, e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", url);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        } catch (IOException e) {
            log.error("Failed to connect to url: {}: {}", url, e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (newsCount != null && newsCount == 1) {
            return List.of(buildFullNewsMessageText(syndFeed.getEntries().get(0)));
        }

        return buildListOfNewsMessageText(syndFeed.getEntries());
    }

    /**
     * Creating formatted list of news from feeds.
     *
     * @param entries feeds of news.
     * @return formatted news.
     */
    private List<String> buildListOfNewsMessageText(List<SyndEntry> entries) {
        List<NewsMessage> newsMessages = entries
                .stream()
                .map(this::buildNewsMessageFromSyndEntry)
                .collect(Collectors.toList());

        return newsMessageService.save(newsMessages)
                .stream()
                .map(this::buildShortNewsMessageText)
                .collect(Collectors.toList());
    }

    public String buildShortNewsMessageText(NewsMessage newsMessage, String sourceName) {
        return "<b>" + newsMessage.getTitle() + "</b> <a href='" + newsMessage.getLink() + "'>(" + sourceName + ")</a>\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    private String buildShortNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    private String buildFullNewsMessageText(SyndEntry syndEntry) {
        return buildFullNewsMessageText(buildNewsMessageFromSyndEntry(syndEntry));
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
