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
import javax.persistence.Table;

/**
 * CommandProperties entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "commandproperties", schema = "bot")
public class CommandProperties {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commandname")
    private String commandName;

    @Column(name = "russifiedname")
    private String russifiedName;

    @Column(name = "enruname")
    private String enRuName;

    @Column(name = "classname")
    private String className;

    @Column(name = "accesslevel")
    private Integer accessLevel;

    @Column(name = "help")
    private String help;
}
