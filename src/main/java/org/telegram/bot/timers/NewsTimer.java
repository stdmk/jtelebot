package org.telegram.bot.timers;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.services.NewsMessageService;
import org.telegram.bot.services.NewsService;
import org.telegram.bot.services.NewsSourceService;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.RssMapper;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsTimer extends TimerParent {

    private final SendMessageExecutor sendMessageExecutor;
    private final NewsService newsService;
    private final NewsMessageService newsMessageService;
    private final NewsSourceService newsSourceService;
    private final RssMapper rssMapper;
    private final NetworkUtils networkUtils;

    @Override
    @Scheduled(fixedRate = 300000)
    public void execute() {
        List<NewsSource> newsSources = newsService.getAll()
                .stream()
                .map(News::getNewsSource)
                .distinct()
                .collect(Collectors.toList());

        newsSources.forEach(newsSource -> {
            SyndFeed syndFeed;
            try {
                syndFeed = networkUtils.getRssFeedFromUrl(newsSource.getUrl());
            } catch (FeedException e) {
                log.error("Failed to parse NewsSource: {}", newsSource.getId(), e);
                return;
            } catch (MalformedURLException e) {
                log.error("Malformed URL: {}", newsSource.getUrl());
                return;
            } catch (IOException e) {
                log.error("Failed to connect to NewsSource: {}", newsSource.getUrl());
                return;
            }

            syndFeed.getEntries().forEach(syndEntry -> {
                NewsMessage newsMessage = rssMapper.toNewsMessage(syndEntry);

                if (newsSource.getNewsMessage() == null || newsSource.getNewsMessage().getPubDate().before(newsMessage.getPubDate())) {
                    newsMessage = newsMessageService.save(newsMessage);
                    newsSource.setNewsMessage(newsMessage);
                    newsSourceService.save(newsSource);
                    NewsMessage finalNewsMessage = newsMessage;
                    newsService.getAll(newsSource)
                            .forEach(news -> {
                                    SendMessage sendMessage = new SendMessage();

                                    sendMessage.setChatId(news.getChat().getChatId().toString());
                                    sendMessage.enableHtml(true);
                                    sendMessage.disableWebPagePreview();
                                    sendMessage.setText(rssMapper.toShortNewsMessageText(finalNewsMessage, news.getNewsSource().getName()));

                                    sendMessageExecutor.executeMethod(sendMessage);
                            });
                }
            });
        });


    }
}
