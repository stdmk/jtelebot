package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * Holiday entity.
 */
@Entity
@Data
@Table(name = "holiday", schema = "bot")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;
}
