package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * ReactionDayStats entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "reactiondaystats", schema = "bot")
public class ReactionDayStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chatid")
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    @Column(name = "emoji")
    private String emoji;

    @Column(name = "count")
    private Integer count;

}
