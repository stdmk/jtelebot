package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * GoogleSearchResult entity.
 */
@Entity
@Data
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
