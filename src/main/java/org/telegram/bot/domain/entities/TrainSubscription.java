package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "trainsubscription", schema = "bot")
public class TrainSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "count_left", nullable = false)
    private Float countLeft;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "period")
    private Period period;

    @Column(name = "active")
    private Boolean active;
}
