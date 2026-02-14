package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Wiki entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "wiki", schema = "bot")
public class Wiki {
    @Id
    @Column(name = "pageid")
    private Integer pageId;

    @Column(name = "title")
    private String title;

    @Column(name = "text")
    private String text;
}
