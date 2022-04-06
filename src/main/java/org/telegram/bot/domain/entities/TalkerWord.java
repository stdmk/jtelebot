package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Set;

/**
 * TalkerWord entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "talkerword", schema = "bot")
public class TalkerWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Long id;

    @Column(name = "word")
    private String word;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "talkerwordphrase",
            joinColumns = @JoinColumn(name = "word_id"),
            inverseJoinColumns = @JoinColumn(name = "phrase_id"))
    private Set<TalkerPhrase> phrases;
}
