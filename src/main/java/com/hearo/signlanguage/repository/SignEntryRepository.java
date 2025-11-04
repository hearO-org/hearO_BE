package com.hearo.signlanguage.repository;

import com.hearo.signlanguage.domain.SignEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface SignEntryRepository extends JpaRepository<SignEntry, Long> {

    Optional<SignEntry> findByLocalId(String localId);

    Page<SignEntry> findByTitleContaining(String keyword, Pageable pageable);

    @Query("select s from SignEntry s where s.localId in :localIds")
    List<SignEntry> findAllByLocalIdIn(@Param("localIds") List<String> localIds);

    default Map<String, SignEntry> findAllByIdOrLocalId(List<String> localIds) {
        if (localIds == null || localIds.isEmpty()) return Map.of();
        var list = findAllByLocalIdIn(localIds);
        var map = new HashMap<String, SignEntry>(list.size() * 2);
        for (var s : list) map.put(s.getLocalId(), s);
        return map;
    }
}
