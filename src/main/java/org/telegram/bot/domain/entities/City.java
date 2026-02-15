package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Column(name = "timezone")
    private String timeZone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user")
    private User user;
}
