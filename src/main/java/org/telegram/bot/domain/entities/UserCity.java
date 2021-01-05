package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * UserCity entity.
 */

@Entity
@Data
@Table(name = "usercity", schema = "bot")
public class UserCity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @OneToOne
    @JoinColumn(name = "cityid")
    private City city;
}
