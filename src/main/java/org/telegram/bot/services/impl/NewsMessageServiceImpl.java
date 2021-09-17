package org.telegram.bot.services.impl;

import com.rometools.rome.feed.synd.SyndEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.repositories.NewsMessageRepository;
import org.telegram.bot.services.NewsMessageService;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.reduceSpaces;
import static org.telegram.bot.utils.TextUtils.cutHtmlTags;
import static org.telegram.bot.utils.DateUtils.formatDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsMessageServiceImpl implements NewsMessageService {

    private final NewsMessageRepository newsMessageRepository;

    @Override
    public NewsMessage get(Long newsId) {
        log.debug("Request to get News by id: {} ", newsId);
        return newsMessageRepository.findById(newsId).orElse(null);
    }

    @Override
    public NewsMessage save(NewsMessage newsMessage) {
        log.debug("Request to save News {} ", newsMessage);
        return newsMessageRepository.save(newsMessage);
    }

    @Override
    public List<NewsMessage> save(List<NewsMessage> newsMessageList) {
        log.debug("Request to save News {} ", newsMessageList);
        return newsMessageRepository.saveAll(newsMessageList);
    }

    @Override
    public String buildShortNewsMessageText(NewsMessage newsMessage, String sourceName) {
        return "<b>" + newsMessage.getTitle() + "</b> (" + sourceName + ")\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    @Override
    public String buildShortNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    @Override
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
                publishedDate = Date.from(Instant.MIN);
            }
        }

        NewsMessage newsMessage = new NewsMessage()
                .setLink(syndEntry.getLink())
                .setTitle(title)
                .setDescription(description)
                .setPubDate(publishedDate);
        if (!syndEntry.getEnclosures().isEmpty()) {
            newsMessage.setAttachUrl(syndEntry.getEnclosures().get(0).getUrl());
        }

        return newsMessage;
    }

    @Override
    public String buildFullNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n" +
                "<i>" + formatDate(newsMessage.getPubDate()) + "</i>\n" +
                newsMessage.getDescription() +
                "\n<a href=\"" + newsMessage.getLink() + "\">Читать полностью</a>";
    }
}
