package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * Speech entity.
 */
@Entity
@Data
@Table(name = "speech", schema = "bot")
public class Speech {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tag")
    private String tag;

    @Column(name = "message")
    private String message;

}
