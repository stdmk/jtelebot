package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * WorkParam entity.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "workparam", schema = "bot")
public class WorkParam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bottoken")
    private String botToken;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    public WorkParam(String botToken, String name) {
        this.botToken = botToken;
        this.name = name;
    }
}
