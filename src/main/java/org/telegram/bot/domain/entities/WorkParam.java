package org.telegram.bot.domain.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * WorkParam entity.
 */
@Entity
@NoArgsConstructor
@Data
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
