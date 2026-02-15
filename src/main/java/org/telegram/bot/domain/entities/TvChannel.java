package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TvChannel entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "tvchannel", schema = "bot")
public class TvChannel {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;
}
