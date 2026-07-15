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
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FamilyGroup group;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GenerationStatus avatarStatus;

    @Column(length = 50)
    private String voicepackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GenerationStatus voiceStatus;

    @Column(length = 255)
    private String greetingAudioUrl;

    @Builder
    private Member(FamilyGroup group, String name) {
        this.group = group;
        this.name = name;
        this.avatarStatus = GenerationStatus.PENDING;
        this.voiceStatus = GenerationStatus.PENDING;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void markAvatarProcessing() {
        this.avatarStatus = GenerationStatus.PROCESSING;
    }

    public void markAvatarReady(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.avatarStatus = GenerationStatus.READY;
    }

    public void markAvatarFailed() {
        this.avatarStatus = GenerationStatus.FAILED;
    }

    public void markVoiceProcessing() {
        this.voiceStatus = GenerationStatus.PROCESSING;
    }

    public void markVoiceReady(String voicepackId) {
        this.voicepackId = voicepackId;
        this.voiceStatus = GenerationStatus.READY;
    }

    public void markVoiceFailed() {
        this.voiceStatus = GenerationStatus.FAILED;
    }

    public void attachGreetingAudio(String greetingAudioUrl) {
        this.greetingAudioUrl = greetingAudioUrl;
    }
}
