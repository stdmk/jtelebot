package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.enums.ReminderRepeatability;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reminder entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "reminder", schema = "bot")
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "text")
    private String text;

    @Column(name = "notified")
    private Boolean notified;

    @Column(name = "repeatability")
    @Enumerated(EnumType.STRING)
    private ReminderRepeatability repeatability;
}
