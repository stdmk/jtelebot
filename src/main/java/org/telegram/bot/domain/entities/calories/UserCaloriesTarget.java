package org.telegram.bot.domain.entities.calories;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.User;

import javax.persistence.*;

/**
 * UserCaloriesTarget entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "usercaloriestarget", schema = "bot")
public class UserCaloriesTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "proteins")
    private Double proteins;

    @Column(name = "fats")
    private Double fats;

    @Column(name = "carbs")
    private Double carbs;

    @Column(name = "caloric")
    private Double calories;

}
