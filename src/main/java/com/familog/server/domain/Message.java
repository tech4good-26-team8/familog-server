package com.familog.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FamilyGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type;

    @Column(length = 500)
    private String text;

    @Column(length = 255)
    private String audioUrl;

    @Column(length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GenerationStatus convertStatus;

    @Builder
    private Message(FamilyGroup group, Member sender, MessageType type,
                    String text, String audioUrl, String imageUrl, GenerationStatus convertStatus) {
        this.group = group;
        this.sender = sender;
        this.type = type;
        this.text = text;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
        this.convertStatus = convertStatus;
    }

    public void attachVoiceAudio(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public void attachImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void markProcessing() {
        this.convertStatus = GenerationStatus.PROCESSING;
    }

    public void completeTts(String audioUrl) {
        this.audioUrl = audioUrl;
        this.convertStatus = GenerationStatus.READY;
    }

    public void completeStt(String text) {
        this.text = text;
        this.convertStatus = GenerationStatus.READY;
    }

    public void markFailed() {
        this.convertStatus = GenerationStatus.FAILED;
    }
}
