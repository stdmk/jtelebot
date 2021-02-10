package org.telegram.bot.services.impl;

import com.rometools.rome.feed.synd.SyndEntry;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.repositories.NewsMessageRepository;
import org.telegram.bot.services.NewsMessageService;

import java.util.List;

import static org.telegram.bot.utils.TextUtils.reduceSpaces;
import static org.telegram.bot.utils.TextUtils.cutHtmlTags;
import static org.telegram.bot.utils.DateUtils.formatDate;

@Service
@AllArgsConstructor
public class NewsMessageServiceImpl implements NewsMessageService {

    private final Logger log = LoggerFactory.getLogger(NewsMessageServiceImpl.class);

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
        NewsMessage newsMessage = new NewsMessage();
        newsMessage.setTitle(syndEntry.getTitle());
        newsMessage.setLink(syndEntry.getLink());

        String description;
        if (syndEntry.getDescription() == null) {
            description = "";
        } else {
            description = cutHtmlTags(syndEntry.getDescription().getValue());
            if (description.length() > 768) {
                description = description.substring(0, 767) + "...";
            }
        }
        newsMessage.setDescription(reduceSpaces(description));
        newsMessage.setPubDate(syndEntry.getPublishedDate());
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
