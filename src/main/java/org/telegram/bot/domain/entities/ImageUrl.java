package org.telegram.bot.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * ImageUrl entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "imageurl", schema = "bot")
public class ImageUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "url")
    private String url;

    @Transient
    private String thumbnailUrl;
}
