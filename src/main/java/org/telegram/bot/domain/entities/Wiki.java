package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
