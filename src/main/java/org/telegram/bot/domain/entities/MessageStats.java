package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * MessageStats entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "messagestats", schema = "bot")
public class MessageStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "messageid", nullable = false)
    private Message message;

    @Column(name = "replies")
    private Integer replies;

    @Column(name = "reactions")
    private Integer reactions;

}
