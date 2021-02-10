package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * TvProgram entity.
 */
@Entity
@Data
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
