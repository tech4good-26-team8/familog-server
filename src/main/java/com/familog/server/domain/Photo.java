package com.familog.server.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class Photo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FamilyGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Member uploader;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private LocalDate takenDate;

    @Builder
    private Photo(FamilyGroup group, Member uploader, String imageUrl, String location, LocalDate takenDate) {
        this.group = group;
        this.uploader = uploader;
        this.imageUrl = imageUrl;
        this.location = location;
        this.takenDate = takenDate;
    }
}
