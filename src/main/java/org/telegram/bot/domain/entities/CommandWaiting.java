package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * CommandWaiting entity.
 */

@Entity
@Data
@Table(name = "commandwaiting", schema = "bot")
public class CommandWaiting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chatid", nullable = false)
    private Long chatId;

    @Column(name = "userid", nullable = false)
    private Integer userId;

    @Column(name = "commandname", nullable = false)
    private String commandName;

    @Column(name = "textmessage")
    private String textMessage;

    @Column(name = "isfinished")
    private Boolean isFinished;
}
