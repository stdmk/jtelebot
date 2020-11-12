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

    @Column(name = "chatid", nullable = false)
    private Long chatId;

    @OneToOne
    @JoinColumn(name = "cityid")
    private City city;
}
