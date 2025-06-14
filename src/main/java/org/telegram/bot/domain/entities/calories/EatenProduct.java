package org.telegram.bot.domain.entities.calories;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * EatenProduct entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString(exclude = "userCalories")
@Table(name = "eatenproduct", schema = "bot")
public class EatenProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "productid", nullable = false)
    private Product product;

    @Column(name = "grams")
    private Double grams;

    @Column(name = "datetime")
    private LocalDateTime dateTime;

    @ManyToOne
    @JoinColumn(name = "usercaloriesid")
    private UserCalories userCalories;

}
