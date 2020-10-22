package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * LastMessage entity.
 */

@Entity
@Data
@Table(name = "lastmessage", schema = "bot")
public class LastMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "messageid")
    private Integer messageId;

    @Column(name = "text")
    private String text;

    @Column(name = "date")
    private LocalDateTime date;
}
