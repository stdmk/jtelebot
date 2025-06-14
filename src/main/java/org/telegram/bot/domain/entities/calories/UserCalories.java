package org.telegram.bot.domain.entities.calories;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.User;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

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
    private List<EatenProduct> eatenProducts;

}
