package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * LastMessage entity.
 */

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
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
