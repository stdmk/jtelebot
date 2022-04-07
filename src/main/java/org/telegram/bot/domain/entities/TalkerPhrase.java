package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Set;

/**
 * TalkerPhrase entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "talkerphrase", schema = "bot")
public class TalkerPhrase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phrase_id")
    private Long id;

    @Column(name = "phrase")
    private String phrase;

    @ManyToMany(mappedBy = "phrases")
    private Set<TalkerWord> talkerWords;
}
