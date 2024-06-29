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
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.NewsMessageService;
import org.telegram.bot.services.NewsService;
import org.telegram.bot.services.NewsSourceService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.RssMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.isThatInteger;
import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class News implements Command {

    private static final List<String> EMPTY_STRING_LIST = Collections.emptyList();
    private static final String WORD_PATTERN_TEMPLATE = "\\b(?:%s)\\b";
    private static final Pattern PARAMS_PATTERN = Pattern.compile(
            "(?:\"([^\"]*?)\"|(\\b\\w+\\b))(?:\\s|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private final Bot bot;
    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final NewsMessageService newsMessageService;
    private final SpeechService speechService;
    private final RssMapper rssMapper;
    private final NetworkUtils networkUtils;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        Chat chat = new Chat().setChatId(message.getChatId());
        ResponseSettings responseSettings = new ResponseSettings()
                .setFormattingStyle(FormattingStyle.HTML)
                .setWebPagePreview(false);

        List<String> responseTextList;
        if (commandArgument == null) {
            responseTextList = getLastNewsForChat(chat);
        } else if (commandArgument.startsWith("_")) {
            NewsMessage newsMessage = getNewsById(commandArgument.substring(1));

            String responseText = rssMapper.toFullNewsMessageText(newsMessage);

            if (newsMessage.getAttachUrl() == null) {
                return returnResponse(new TextResponse(message)
                        .setText(responseText)
                        .setResponseSettings(responseSettings));
            }

            return returnResponse(new FileResponse(message)
                    .addFile(new File(FileType.IMAGE, newsMessage.getAttachUrl()))
                    .setText(responseText)
                    .setResponseSettings(responseSettings));
        } else {
            log.debug("Request to get news from {}", commandArgument);

            org.telegram.bot.domain.entities.News news = newsService.get(chat, commandArgument);

            String url;
            Integer count = null;
            if (news != null) {
                url = news.getNewsSource().getUrl();
            } else {
                int spaceIndex = commandArgument.indexOf(" ");
                if (spaceIndex > 0) {
                    url = commandArgument.substring(0, spaceIndex);
                    String potentialCount = commandArgument.substring(spaceIndex + 1);
                    if (isThatInteger(potentialCount)) {
                        count = Integer.parseInt(potentialCount);
                    }
                } else {
                    url = commandArgument;
                }
            }

            if (isThatUrl(url)) {
                checkNewsCount(count);
                responseTextList = getAllNews(url, count);
            } else {
                responseTextList = searchForNews(commandArgument);
            }
        }

        if (responseTextList.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return mapToTextResponseList(responseTextList, message, responseSettings);
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
                .map(newsSource -> {
                    try {
                        return searchNewsInNewsSource(newsSource, wordsPattern);
                    } catch (Exception e) {
                        return EMPTY_STRING_LIST;
                    }
                })
                .flatMap(Collection::stream)
                .filter(StringUtils::hasText)
                .toList();
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
        SyndFeed syndFeed = getSyndFeedFromUrl(newsSource.getUrl());

        List<NewsMessage> newsMessageList = syndFeed.getEntries()
                .stream()
                .filter(syndEntry -> entryMatchesPattern(syndEntry, pattern))
                .map(rssMapper::toNewsMessage)
                .toList();

        return newsMessageService.save(newsMessageList)
                .stream()
                .map(newsMessage -> rssMapper.toShortNewsMessageText(newsMessage, newsSource.getName()))
                .toList();
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
                        return rssMapper.toShortNewsMessageText(newsMessage, news.getNewsSource().getName());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList());

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
        SyndFeed syndFeed = getSyndFeedFromUrl(url);

        if (newsCount != null && newsCount == 1) {
            return List.of(rssMapper.toFullNewsMessageText(syndFeed.getEntries().get(0)));
        }

        return newsMessageService.save(rssMapper.toNewsMessage(syndFeed.getEntries()))
                .stream()
                .map(rssMapper::toShortNewsMessageText)
                .toList();

    }

    private SyndFeed getSyndFeedFromUrl(String url) {
        try {
            return networkUtils.getRssFeedFromUrl(url);
        } catch (FeedException e) {
            log.error("Failed to parse news from url: {}: {}", url, e.getMessage());
            botStats.incrementErrors(url, e, "Unable to read rss feed");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", url);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        } catch (IOException e) {
            log.error("Failed to connect to url: {}: {}", url, e.getMessage());
            botStats.incrementErrors(url, e, "Failed to connect to url");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

}
