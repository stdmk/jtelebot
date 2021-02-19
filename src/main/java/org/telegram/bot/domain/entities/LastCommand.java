package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * LastCommand entity.
 */
@Entity
@Data
@Table(name = "lastcommand", schema = "bot")
public class LastCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @OneToOne
    @JoinColumn(name = "commandpropertiesid", nullable = false)
    private CommandProperties commandProperties;
}
