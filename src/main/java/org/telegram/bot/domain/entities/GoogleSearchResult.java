package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * GoogleSearchResult entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "googlesearchresult", schema = "bot")
public class GoogleSearchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "link")
    private String link;

    @Column(name = "displaylink")
    private String displayLink;

    @Column(name = "snippet")
    private String snippet;

    @Column(name = "formattedurl")
    private String formattedUrl;

    @ManyToOne
    @JoinColumn(name = "imageurlid")
    private ImageUrl imageUrl;
}
