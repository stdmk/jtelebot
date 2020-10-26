package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Timer entity.
 */
@Entity
@Data
@Table(name = "timer", schema = "bot")
public class Timer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "lastalarmdt")
    private LocalDateTime lastAlarmDt;
}
