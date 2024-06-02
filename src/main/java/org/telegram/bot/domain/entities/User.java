package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * User entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "user", schema = "bot")
public class User {
    @Id
    @Column(name = "userid")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "accesslevel")
    private Integer accessLevel;

    @Transient
    private String lang;
}
