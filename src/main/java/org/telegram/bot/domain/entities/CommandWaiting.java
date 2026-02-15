package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * CommandWaiting entity.
 */

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
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
