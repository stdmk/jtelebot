package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.bot.enums.ChatGPTRole;

import javax.persistence.*;

/**
 * ChatGPTMessage entity.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "chatgptmessage", schema = "bot")
public class ChatGPTMessage {
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
    private ChatGPTRole role;

    @Column(name = "content")
    private String content;
}
