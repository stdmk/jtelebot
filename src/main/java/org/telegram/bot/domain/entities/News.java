package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * News entity.
 */
@Entity
@Data
@Table(name = "news", schema = "bot")
public class News {
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

    @Column(name = "pubDate")
    private String pubDate;

    @Column(name = "attachurl")
    private String attachUrl;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @Column(name = "messageid")
    private Integer messageId;

    @ManyToOne
    @JoinColumn(name = "newssourceid")
    private NewsSource newsSource;
}
