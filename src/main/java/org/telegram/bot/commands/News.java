package org.telegram.bot.commands;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
import java.util.stream.LongStream;

import static org.telegram.bot.utils.TextUtils.isThatPositiveInteger;
import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class News implements Command {

    private static final int MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT = 10;
    private static final String WORD_PATTERN_TEMPLATE = "\\b(?:%s)\\b";
    private static final Pattern GET_ARRAY_OF_NEWS_PATTERN = Pattern.compile("_(\\d+)_(\\d+)");
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
            Matcher matcher = GET_ARRAY_OF_NEWS_PATTERN.matcher(commandArgument);
            if (matcher.find()) {
                responseTextList = getNewsByIds(matcher.group(1), matcher.group(2));
            } else {
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
            }
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
                    if (isThatPositiveInteger(potentialCount)) {
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

        Map<NewsSource, List<NewsMessage>> sourceNewsMessageMap = new HashMap<>();
        newsSourceService.getAll()
                .forEach(newsSource -> {
                    List<NewsMessage> newsMessages = searchNewsInNewsSource(newsSource, wordsPattern);
                    if (!newsMessages.isEmpty()) {
                        sourceNewsMessageMap.put(newsSource, newsMessages);
                    }
                });

        List<String> response = new ArrayList<>(MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT + 1);
        NewsMessage lastAddedToResponseNewsMessage = null;
        for (Map.Entry<NewsSource, List<NewsMessage>> entry : sourceNewsMessageMap.entrySet()) {
            NewsSource source = entry.getKey();
            List<NewsMessage> newsMessages = entry.getValue();
            NewsMessage firstNewsMessageOfSource = newsMessages.get(0);

            if (response.size() == MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT) {
                List<NewsMessage> allFoundMessages = sourceNewsMessageMap.values().stream().flatMap(Collection::stream).toList();
                response.add(getNextMessagesArrayCommand(lastAddedToResponseNewsMessage.getId() + 1, allFoundMessages.size() - 1));
                break;
            }

            String moreNews = "";
            if (newsMessages.size() > 1) {
                if (newsMessages.size() == 2) {
                    moreNews = "${command.news.morenews}:" + " /news_" + newsMessages.get(1).getId();
                } else {
                    moreNews = getNextMessagesArrayCommand(newsMessages.get(1).getId(), newsMessages.get(newsMessages.size() - 1).getId());
                }
            }

            response.add(rssMapper.toShortNewsMessageText(firstNewsMessageOfSource, source.getName(), moreNews));
            lastAddedToResponseNewsMessage = firstNewsMessageOfSource;
        }

        if (response.size() < MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT) {
            for (Map.Entry<NewsSource, List<NewsMessage>> entry : sourceNewsMessageMap.entrySet()) {
                if (response.size() == MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT) {
                    break;
                }

                List<NewsMessage> newsMessages = entry.getValue();
                if (newsMessages.size() < 2) {
                    continue;
                }

                for (int i = 1; i < newsMessages.size(); i++) {
                    NewsMessage newsMessage = newsMessages.get(i);
                    response.add(rssMapper.toShortNewsMessageText(newsMessage, entry.getKey().getName()));
                    if (response.size() == MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT) {
                        break;
                    }
                }
            }
        }

        return response;
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

    private List<NewsMessage> searchNewsInNewsSource(NewsSource newsSource, Pattern pattern) {
        SyndFeed syndFeed;

        try {
            syndFeed = getSyndFeedFromUrl(newsSource.getUrl());
        } catch (Exception e) {
            return Collections.emptyList();
        }

        List<NewsMessage> newsMessageList = syndFeed.getEntries()
                .stream()
                .filter(syndEntry -> entryMatchesPattern(syndEntry, pattern))
                .map(rssMapper::toNewsMessage)
                .toList();

        return newsMessageService.save(newsMessageList);
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

    private List<String> getNewsByIds(String startOfArrayId, String endOfArrayId) {
        long newsIdStart;
        long newsIdEnd;
        try {
            newsIdStart = Long.parseLong(startOfArrayId);
            newsIdEnd = Long.parseLong(endOfArrayId);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        long countOffAllNews = newsMessageService.getLastNewsMessage().getId();
        if (countOffAllNews < newsIdEnd || newsIdEnd - newsIdStart <= 0 || newsIdEnd - newsIdStart > MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        List<Long> ids = LongStream.range(newsIdStart, newsIdEnd + 1).boxed().toList();
        List<NewsMessage> newsMessages = newsMessageService.getAll(ids);
        if (newsMessages.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        List<String> response = newsMessages.stream().map(rssMapper::toShortNewsMessageText).collect(Collectors.toList());
        if (countOffAllNews != newsIdEnd) {
            response.add(getNextMessagesArrayCommand(newsIdEnd + 1, countOffAllNews));
        }

        return response;
    }

    private String getNextMessagesArrayCommand(long start, long count) {
        long end;
        if (count - start > MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT) {
            end = start + MAX_FOUND_NEWS_MESSAGES_IN_MESSAGE_COUNT - 1;
        } else {
            end = count;
        }

        return "${command.news.morenews}:" + " /news_" + start + "_" + end;
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
