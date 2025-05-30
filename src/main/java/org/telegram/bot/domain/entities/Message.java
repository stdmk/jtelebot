package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.model.request.MessageContentType;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Message entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "message", schema = "bot")
public class Message {

    @Id
    @Column(name = "messageid")
    private Integer messageId;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private MessageContentType messageContentType;

    @Column(name = "text")
    private String text;

    @Column(name = "datetime")
    private LocalDateTime dateTime;

}
