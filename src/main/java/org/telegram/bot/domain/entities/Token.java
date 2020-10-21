package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * Token entity.
 */
@Entity
@Data
@Table(name = "token", schema = "bot")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token")
    private String token;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

}
