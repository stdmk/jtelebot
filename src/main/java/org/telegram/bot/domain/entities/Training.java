package org.telegram.bot.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@EqualsAndHashCode
@Table(name = "training", schema = "bot")
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "name")
    private String name;

    @Column(name = "cost")
    private Float cost;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;
}
