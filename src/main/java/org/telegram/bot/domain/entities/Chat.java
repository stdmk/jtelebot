package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * Chat entity.
 */
@Entity
@Data
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
