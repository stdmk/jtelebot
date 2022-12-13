package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Set;

/**
 * TrackCode entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "trackcode", schema = "bot")
public class TrackCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_code_id")
    private Long id;

    @Column(name = "barcode")
    private String barcode;

    @OneToMany(mappedBy = "trackCode", fetch = FetchType.EAGER)
    private Set<TrackCodeEvent> events;
}
