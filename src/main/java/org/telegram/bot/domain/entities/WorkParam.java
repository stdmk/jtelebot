package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
