package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * Alias entity.
 */

@Entity
@Data
@Table(name = "alias", schema = "bot")
public class Alias {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;
}
