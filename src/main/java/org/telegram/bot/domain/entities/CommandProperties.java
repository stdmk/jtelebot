package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * CommandProperties entity.
 */
@Entity
@Data
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
