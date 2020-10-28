package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * NewsSource entity.
 */
@Entity
@Data
@Table(name = "newssource", schema = "bot")
public class NewsSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "url")
    private String url;
}
