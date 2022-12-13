package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * TrackCodeEvent entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@Table(name = "trackcodeevent", schema = "bot")
public class TrackCodeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_code_event_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "track_code_id", nullable = false)
    private TrackCode trackCode;

    @Column(name = "event_barcode")
    private String eventBarcode;

    @Column(name = "event_date_time")
    LocalDateTime eventDateTime;

    @Column(name = "address")
    private String address;

    @Column(name = "index")
    private String index;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "gram")
    private Long gram;

    @Column(name = "operation_type")
    private String operationType;

    @Column(name = "operation_desc")
    private String operationDescription;

    @Column(name = "country_from")
    private String countryFrom;

    @Column(name = "country_to")
    private String countryTo;

    @Column(name = "sender")
    private String sender;

    @Column(name = "recipient")
    private String recipient;

}
