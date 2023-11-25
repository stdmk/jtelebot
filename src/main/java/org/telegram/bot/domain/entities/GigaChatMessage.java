package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.enums.GigaChatRole;

import javax.persistence.*;

/**
 * GigaChatMessage entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "gigachatmessage", schema = "bot")
public class GigaChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chatid")
    private Chat chat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userid")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private GigaChatRole role;

    @Column(name = "content")
    private String content;
}
