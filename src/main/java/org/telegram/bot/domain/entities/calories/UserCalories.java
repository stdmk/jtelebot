package org.telegram.bot.domain.entities.calories;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.User;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * UserCalories entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "usercalories", schema = "bot")
public class UserCalories {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "date")
    private LocalDate date;

    @OneToMany(mappedBy = "userCalories", fetch = FetchType.EAGER)
    private Set<EatenProduct> eatenProducts = new HashSet<>();

    @OneToMany(mappedBy = "userCalories", fetch = FetchType.EAGER)
    private Set<Activity> activities = new HashSet<>();

}
