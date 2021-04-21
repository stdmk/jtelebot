package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * File entity.
 */
@Entity
@Data
@Table(name = "file", schema = "bot")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_unique_id")
    private String fileUniqueId;

    @Column(name = "file_id")
    private String fileId;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatid")
    private Chat chat;

    @Column(name = "name")
    private String name;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "size")
    private Integer size;

    @Column(name = "type")
    private String type;

    @Column(name = "parent")
    private Long parentId;
}
