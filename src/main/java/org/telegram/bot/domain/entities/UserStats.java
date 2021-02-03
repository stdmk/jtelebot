package org.telegram.bot.domain.entities;

import lombok.Data;

import javax.persistence.*;

/**
 * UserStats entity.
 */

@Entity
@Data
@Table(name = "userstats", schema = "bot")
public class UserStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatid", nullable = false)
    private Chat chat;

    @Column(name = "numberofmessages")
    private Integer numberOfMessages;

    @Column(name = "numberofallmessages")
    private Long numberOfAllMessages;

    @Column(name = "numberofstickers")
    private Integer numberOfStickers;

    @Column(name = "numberofallstickers")
    private Long numberOfAllStickers;

    @Column(name = "numberofphotos")
    private Integer numberOfPhotos;

    @Column(name = "numberofallphotos")
    private Long numberOfAllPhotos;

    @Column(name = "numberofanimations")
    private Integer numberOfAnimations;

    @Column(name = "numberofallanimations")
    private Long numberOfAllAnimations;

    @Column(name = "numberofaudio")
    private Integer numberOfAudio;

    @Column(name = "numberofallaudio")
    private Long numberOfAllAudio;

    @Column(name = "numberofdocuments")
    private Integer numberOfDocuments;

    @Column(name = "numberofalldocuments")
    private Long numberOfAllDocuments;

    @Column(name = "numberofvideos")
    private Integer numberOfVideos;

    @Column(name = "numberofallvideos")
    private Long numberOfAllVideos;

    @Column(name = "numberofvideonotes")
    private Integer numberOfVideoNotes;

    @Column(name = "numberofallvideonotes")
    private Long numberOfAllVideoNotes;

    @Column(name = "numberofvoices")
    private Integer numberOfVoices;

    @Column(name = "numberofallvoices")
    private Long numberOfAllVoices;

    @Column(name = "numberofcommands")
    private Integer numberOfCommands;

    @Column(name = "numberofallcommands")
    private Long numberOfAllCommands;

    @OneToOne
    @JoinColumn(name = "lastmessage")
    private LastMessage lastMessage;

    @Column(name = "karma")
    private Integer numberOfKarma;

    @Column(name = "goodness")
    private Integer numberOfGoodness;
}
