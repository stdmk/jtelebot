package org.telegram.bot.domain.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Table(name = "error", schema = "bot")
public class Error {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_time")
    private LocalDateTime dateTime;

    @Column(name = "request")
    private String request;

    @Column(name = "response")
    private String response;

    @Column(name = "comment")
    private String comment;

    @Column(name = "stacktrace")
    private String stacktrace;
}
