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

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "newssourceid", nullable = false)
    private NewsSource newsSource;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;
}
