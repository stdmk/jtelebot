package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

/**
 * News entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "newsmessage", schema = "bot")
public class NewsMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "link")
    private String link;

    @Column(name = "description")
    private String description;

    @Column(name = "pubdate")
    private Date pubDate;

    @Column(name = "attachurl")
    private String attachUrl;

    /**
     * @deprecated use is not suitable, since the news may belong to different chats.
     */
    @Deprecated
    @Column(name = "messageid")
    private Integer messageId;

    @Column(name = "deschash")
    private String descHash;

}
