package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * TvChannel entity.
 */
@Entity
@Data
@Table(name = "tvchannel", schema = "bot")
public class TvChannel {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;
}
