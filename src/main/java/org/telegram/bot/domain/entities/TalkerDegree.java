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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * TalkerDegree entity.
 */

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "talkerdegree", schema = "bot")
public class TalkerDegree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @Column(name = "degree")
    private Integer degree;

    @Column(name = "chat_idle_minutes")
    private Integer chatIdleMinutes;
}
