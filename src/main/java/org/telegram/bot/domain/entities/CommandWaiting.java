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

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "commandname", nullable = false)
    private String commandName;

    @Column(name = "textmessage")
    private String textMessage;

    @Column(name = "isfinished")
    private Boolean isFinished;
}
