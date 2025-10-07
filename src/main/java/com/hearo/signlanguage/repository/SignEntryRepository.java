package com.hearo.signlanguage.repository;

import com.hearo.signlanguage.domain.SignEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignEntryRepository extends JpaRepository<SignEntry, Long> {

    Optional<SignEntry> findByLocalId(String localId);

    Page<SignEntry> findByTitleContaining(String keyword, Pageable pageable);
}
