package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "useremail", schema = "bot")
public class UserEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Column(name = "email")
    private String email;

    @Column(name = "verified")
    private Boolean verified;

    @Column(name = "shipping_enabled")
    private Boolean shippingEnabled;
}
