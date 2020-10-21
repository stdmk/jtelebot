package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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

    @Column(name = "accesslevel")
    private Integer accessLevel;
}
