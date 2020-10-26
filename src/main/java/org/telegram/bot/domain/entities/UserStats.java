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
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "chatid")
    private Long chatId;

    @Column(name = "numberofmessages")
    private Integer numberOfMessages;

    @Column(name = "numberofallmessages")
    private Long numberOfAllMessages;

    @OneToOne
    @JoinColumn(name = "lastmessage")
    private LastMessage lastMessage;
}
