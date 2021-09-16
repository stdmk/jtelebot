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
 * Chat entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "chat", schema = "bot")
public class Chat {
    @Id
    @Column(name = "chatid")
    private Long chatId;

    @Column(name = "name")
    private String name;

    @Column(name = "accesslevel")
    private Integer accessLevel;
}
