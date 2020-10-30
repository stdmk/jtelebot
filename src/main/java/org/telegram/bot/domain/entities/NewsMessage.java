package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * News entity.
 */
@Entity
@Data
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

    @Column(name = "messageid")
    private Integer messageId;
}
