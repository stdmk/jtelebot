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
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "accesslevel")
    private Integer accessLevel;
}
