package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.NewsMessage;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.*;

@Component
public class RssMapper {

    public List<NewsMessage> toNewsMessage(List<SyndEntry> entries) {
        return entries
                .stream()
                .map(this::toNewsMessage)
                .toList();
    }

    public String toShortNewsMessageText(NewsMessage newsMessage, String sourceName) {
        return toShortNewsMessageText(newsMessage, sourceName, "");
    }

    public String toShortNewsMessageText(NewsMessage newsMessage, String sourceName, String moreNews) {
        if (moreNews != null && !moreNews.isEmpty()) {
            moreNews = "\n" + moreNews;
        }

        return "<b>" + newsMessage.getTitle() + " </b>" + buildHtmlLink(newsMessage.getLink(), sourceName) + "\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + moreNews + "\n\n";
    }

    public String toShortNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n<i>" +
                formatDate(newsMessage.getPubDate()) + "</i> /news_" + newsMessage.getId() + "\n\n";
    }

    public NewsMessage toNewsMessage(SyndEntry syndEntry) {
        String title = reduceSpaces(cutHtmlTags(syndEntry.getTitle()));
        if (title.length() > 255) {
            int i = title.indexOf(".");
            if (i < 0 || i > 255) {
                title = title.substring(0, 50) + "...";
            } else {
                title = title.substring(0, i);
            }
        }

        Date publishedDate = syndEntry.getPublishedDate();
        if (publishedDate == null) {
            publishedDate = syndEntry.getUpdatedDate();
            if (publishedDate == null) {
                publishedDate = Date.from(Instant.now());
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

        String descHash = DigestUtils.sha256Hex(description + title + syndEntry.getLink());

        return new NewsMessage()
                .setLink(syndEntry.getLink())
                .setTitle(title)
                .setDescription(description)
                .setPubDate(publishedDate)
                .setAttachUrl(getAttachUrl(syndEntry))
                .setDescHash(descHash);
    }

    public String toFullNewsMessageText(SyndEntry syndEntry) {
        return toFullNewsMessageText(toNewsMessage(syndEntry));
    }

    public String toFullNewsMessageText(NewsMessage newsMessage) {
        return "<b>" + newsMessage.getTitle() + "</b>\n" +
                "<i>" + formatDate(newsMessage.getPubDate()) + "</i>\n" +
                newsMessage.getDescription() +
                "\n" + TextUtils.buildHtmlLink(newsMessage.getLink(), "${command.news.readcompletely}");
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
