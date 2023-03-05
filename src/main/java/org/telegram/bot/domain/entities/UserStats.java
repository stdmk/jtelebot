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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * UserStats entity.
 */

@Entity
@Getter
@Setter
@Accessors(chain = true)
@ToString
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

    @Column(name = "numberofmessagesperday")
    private Integer numberOfMessagesPerDay;

    @Column(name = "numberofallmessages")
    private Long numberOfAllMessages;

    @Column(name = "numberofstickers")
    private Integer numberOfStickers;

    @Column(name = "numberofstickersperday")
    private Integer numberOfStickersPerDay;

    @Column(name = "numberofallstickers")
    private Long numberOfAllStickers;

    @Column(name = "numberofphotos")
    private Integer numberOfPhotos;

    @Column(name = "numberofphotosperday")
    private Integer numberOfPhotosPerDay;

    @Column(name = "numberofallphotos")
    private Long numberOfAllPhotos;

    @Column(name = "numberofanimations")
    private Integer numberOfAnimations;

    @Column(name = "numberofanimationsperday")
    private Integer numberOfAnimationsPerDay;

    @Column(name = "numberofallanimations")
    private Long numberOfAllAnimations;

    @Column(name = "numberofaudio")
    private Integer numberOfAudio;

    @Column(name = "numberofaudioperday")
    private Integer numberOfAudioPerDay;

    @Column(name = "numberofallaudio")
    private Long numberOfAllAudio;

    @Column(name = "numberofdocuments")
    private Integer numberOfDocuments;

    @Column(name = "numberofdocumentsperday")
    private Integer numberOfDocumentsPerDay;

    @Column(name = "numberofalldocuments")
    private Long numberOfAllDocuments;

    @Column(name = "numberofvideos")
    private Integer numberOfVideos;

    @Column(name = "numberofvideosperday")
    private Integer numberOfVideosPerDay;

    @Column(name = "numberofallvideos")
    private Long numberOfAllVideos;

    @Column(name = "numberofvideonotes")
    private Integer numberOfVideoNotes;

    @Column(name = "numberofvideonotesperday")
    private Integer numberOfVideoNotesPerDay;

    @Column(name = "numberofallvideonotes")
    private Long numberOfAllVideoNotes;

    @Column(name = "numberofvoices")
    private Integer numberOfVoices;

    @Column(name = "numberofvoicesperday")
    private Integer numberOfVoicesPerDay;

    @Column(name = "numberofallvoices")
    private Long numberOfAllVoices;

    @Column(name = "numberofcommands")
    private Integer numberOfCommands;

    @Column(name = "numberofcommandsperday")
    private Integer numberOfCommandsPerDay;

    @Column(name = "numberofallcommands")
    private Long numberOfAllCommands;

    @OneToOne
    @JoinColumn(name = "lastmessage")
    private LastMessage lastMessage;

    @Column(name = "numberofkarma")
    private Integer numberOfKarma;

    @Column(name = "numberofkarmaperday")
    private Integer numberOfKarmaPerDay;

    @Column(name = "numberofallkarma")
    private Long numberOfAllKarma;

    @Column(name = "numberofgoodness")
    private Integer numberOfGoodness;

    @Column(name = "numberofgoodnessperday")
    private Integer numberOfGoodnessPerDay;

    @Column(name = "numberofallgoodness")
    private Long numberOfAllGoodness;

    @Column(name = "numberofwickedness")
    private Integer numberOfWickedness;

    @Column(name = "numberofwickednessperday")
    private Integer numberOfWickednessPerDay;

    @Column(name = "numberofallwickedness")
    private Long numberOfAllWickedness;
}
