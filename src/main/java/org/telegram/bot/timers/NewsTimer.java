package org.telegram.bot.timers;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.services.NewsMessageService;
import org.telegram.bot.services.NewsService;
import org.telegram.bot.services.NewsSourceService;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsTimer extends TimerParent {

    private final ApplicationContext context;
    private final NewsService newsService;
    private final NewsMessageService newsMessageService;
    private final NewsSourceService newsSourceService;
    private final NetworkUtils networkUtils;

    @Override
    @Scheduled(fixedRate = 60000)
    public void execute() {
        Bot bot = (Bot) context.getBean("bot");
        List<NewsSource> newsSources = newsService.getAll()
                .stream()
                .map(News::getNewsSource)
                .distinct()
                .collect(Collectors.toList());

        for (NewsSource newsSource : newsSources) {
            SyndFeed syndFeed = networkUtils.getRssFeedFromUrl(newsSource.getUrl());
            for (SyndEntry syndEntry : syndFeed.getEntries()) {
                NewsMessage newsMessage = newsMessageService.buildNewsMessageFromSyndEntry(syndEntry);

                if (newsSource.getNewsMessage() == null || newsSource.getNewsMessage().getPubDate().before(newsMessage.getPubDate())) {
                    newsMessage = newsMessageService.save(newsMessage);
                    newsSource.setNewsMessage(newsMessage);
                    newsSourceService.save(newsSource);
                    NewsMessage finalNewsMessage = newsMessage;
                    for (News news : newsService.getAll(newsSource)) {
                        try {
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(news.getChat().getChatId().toString());
                            sendMessage.enableHtml(true);
                            sendMessage.disableWebPagePreview();
                            sendMessage.setText(newsMessageService.buildShortNewsMessageText(finalNewsMessage, news.getName()));

                            bot.execute(sendMessage);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


    }
}
