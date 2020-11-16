package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * City entity.
 */
@Entity
@Data
@Table(name = "city", schema = "bot")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nameru")
    private String nameRu;

    @Column(name = "nameen")
    private String nameEn;

    @Column(name = "timezone")
    private String timeZone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user")
    private User user;
}
