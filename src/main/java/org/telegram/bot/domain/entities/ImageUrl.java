package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * ImageUrl entity.
 */
@Entity
@Data
@Table(name = "imageurl", schema = "bot")
public class ImageUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "url")
    private String url;
}
