package org.telegram.bot.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * City entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "city", schema = "bot")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nameru")
    private String nameRu;

    @Column(name = "nameen")
    private String nameEn;

    @Deprecated
    @Column(name = "timezone")
    private String timeZone;

    @Column(name = "zoneid")
    private String zoneId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user")
    private User user;
}
