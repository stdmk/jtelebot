package org.telegram.bot.domain.commands;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.enums.BotSpeechTag;
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
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class News implements CommandParent<PartialBotApiMethod<?>> {

    private final Logger log = LoggerFactory.getLogger(News.class);

    private final NewsService newsService;
    private final NewsMessageService newsMessageService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        Chat chat = new Chat().setChatId(message.getChatId());
        String responseText;

        if (textMessage == null) {
            log.debug("Request to get last news for chat {}", chat.getChatId());
            final StringBuilder buf = new StringBuilder();
            buf.append("<b>Последние новости:</b>\n\n");
            newsService.getAll(chat)
                    .forEach(news -> {
                        NewsMessage newsMessage = news.getNewsSource().getNewsMessage();
                        if (newsMessage != null) {
                            buf.append(newsMessageService.buildShortNewsMessageText(news.getNewsSource().getNewsMessage(), news.getName()));
                        }
                    });
            responseText = buf.toString();
        } else if (textMessage.startsWith("_")) {
            long newsId;
            try {
                newsId = Long.parseLong(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            log.debug("Request to get details of news by id {}", newsId);
            NewsMessage newsMessage = newsMessageService.get(newsId);
            if (newsMessage == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = newsMessageService.buildFullNewsMessageText(newsMessage);
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
            if (news != null) {
                url = news.getNewsSource().getUrl();
            } else {
                try {
                    url = new URL(textMessage).toString();
                } catch (MalformedURLException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            }
            responseText = getAllNews(url);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Getting formatted list of news from url.
     *
     * @param url url of rss resource.
     * @return formatted news.
     */
    private String getAllNews(String url) {
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

        final int newsLimit = 10;

        return buildListOfNewsMessageText(syndFeed.getEntries().stream().limit(newsLimit).collect(Collectors.toList()));
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
                .map(newsMessageService::buildNewsMessageFromSyndEntry)
                .collect(Collectors.toList());

        newsMessages = newsMessageService.save(newsMessages);

        StringBuilder buf = new StringBuilder();
        newsMessages.forEach(newsMessage -> buf.append(newsMessageService.buildShortNewsMessageText(newsMessage)));

        return buf.toString();
    }
}
