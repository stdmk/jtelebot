package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * ImageUrl entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
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
