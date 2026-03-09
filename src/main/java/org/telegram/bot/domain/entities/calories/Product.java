package org.telegram.bot.domain.entities.calories;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.entities.User;

/**
 * Product entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "product", schema = "bot")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "name")
    private String name;

    @Column(name = "proteins")
    private double proteins;

    @Column(name = "fats")
    private double fats;

    @Column(name = "carbs")
    private double carbs;

    @Column(name = "fibers")
    private double fibers;

    @Column(name = "caloric")
    private double caloric;

    @Column(name = "deleted")
    private Boolean deleted = false;

}
