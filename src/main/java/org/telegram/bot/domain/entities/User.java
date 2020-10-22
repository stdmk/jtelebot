package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * User entity.
 */
@Entity
@Data
@Table(name = "user", schema = "bot")
public class User {
    @Id
    @Column(name = "userid")
    private Integer userId;

    @Column(name = "username")
    private String username;

    @Column(name = "accesslevel")
    private Integer accessLevel;

    @OneToOne
    @JoinColumn(name="userstats")
    private UserStats userStats;
}
