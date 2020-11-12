package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.util.TimeZone;

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

    @Column(name = "nameRu")
    private String nameRu;

    @Column(name = "nameEn")
    private String nameEn;

    @Column(name = "timezone")
    private String timeZone;
}
