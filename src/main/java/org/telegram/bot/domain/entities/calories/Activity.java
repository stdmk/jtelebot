package org.telegram.bot.domain.entities.calories;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.User;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Activity entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "activity", schema = "bot")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "datetime")
    private LocalDateTime dateTime;

    @Column(name = "name")
    private String name;

    @Column(name = "calories")
    private double calories;

    @ManyToOne
    @JoinColumn(name = "usercaloriesid")
    private UserCalories userCalories;

}
