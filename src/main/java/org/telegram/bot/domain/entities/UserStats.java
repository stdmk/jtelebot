package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * UserStats entity.
 */

@Entity
@Data
@Table(name = "userstats", schema = "bot")
public class UserStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatid")
    private Chat chat;

    @Column(name = "numberofmessages")
    private Integer numberOfMessages;

    @Column(name = "numberofallmessages")
    private Long numberOfAllMessages;

    @ManyToOne
    @JoinColumn(name = "lastMessage")
    private LastMessage lastMessage;
}
