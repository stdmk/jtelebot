package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * CustomReactionMonthStats entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "customreactionmonthstats", schema = "bot")
public class CustomReactionMonthStats {

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

    @Column(name = "emojiid")
    private String emojiId;

    @Column(name = "count")
    private Integer count;

}
