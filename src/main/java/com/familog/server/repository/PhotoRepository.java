package com.familog.server.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.familog.server.domain.Photo;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Photo> findByGroupIdAndTakenDateOrderByCreatedAtDesc(Long groupId, LocalDate takenDate);
}
