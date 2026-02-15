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
import java.time.LocalDateTime;

/**
 * TvProgram entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "tvprogram", schema = "bot")
public class TvProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "channelid", nullable = false)
    private TvChannel channel;

    @Column(name = "title")
    private String title;

    @Column(name = "category")
    private String category;

    @Column(name = "desc")
    private String desc;

    @Column(name = "start")
    private LocalDateTime start;

    @Column(name = "stop")
    private LocalDateTime stop;
}
