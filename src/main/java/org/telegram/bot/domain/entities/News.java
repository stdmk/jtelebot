package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

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

    @Column(name = "pubdate")
    private Date pubDate;

    @Column(name = "attachurl")
    private String attachUrl;

    @Column(name = "messageid")
    private Integer messageId;

    @ManyToOne
    @JoinColumn(name = "newssourceid")
    private NewsSource newsSource;
}
