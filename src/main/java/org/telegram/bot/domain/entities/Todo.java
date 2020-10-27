package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * Todo entity.
 */
@Entity
@Data
@Table(name = "todo", schema = "bot")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "todotext")
    private String todoText;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;
}
