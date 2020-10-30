package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * NewsSource entity.
 */
@Entity
@Data
@Table(name = "newssource", schema = "bot")
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "url")
    private String url;

    @ManyToOne
    @JoinColumn(name = "newsmessageid")
    private NewsMessage newsMessage;
}
