package com.hearo.signlanguage.service;

import com.hearo.signlanguage.client.SignApiClient;
import com.hearo.signlanguage.client.dto.SignRawResponse;
import com.hearo.signlanguage.domain.SignEntry;
import com.hearo.signlanguage.dto.IngestResultDto;
import com.hearo.signlanguage.dto.SignItemDto;
import com.hearo.signlanguage.dto.SignPageDto;
import com.hearo.signlanguage.repository.SignEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SignService {

    private final SignApiClient client;
    private final SignEntryRepository repo;

    public SignPageDto externalList(int pageNo, int numOfRows) {
        SignRawResponse raw = client.fetch(null, pageNo, numOfRows);
        return toPageDto(raw);
    }

    public SignPageDto externalSearch(String keyword, int pageNo, int numOfRows) {
        SignRawResponse raw = client.fetch(keyword, pageNo, numOfRows);
        return toPageDto(raw);
    }

    public Page<SignEntry> listFromDb(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("id").descending());
        return repo.findAll(pageable);
    }

    public Page<SignEntry> searchFromDb(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("id").descending());
        return repo.findByTitleContaining(keyword, pageable);
    }

    @Transactional
    public IngestResultDto ingestAll(int pageSize) {
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        int totalFetched = 0, inserted = 0, updated = 0;

        while (true) {
            SignPageDto page = fetchPage(pageNo, pageSize);
            if (pageNo == 1) totalCount = page.getTotalCount();
            if (page.getItems().isEmpty()) break;

            for (SignItemDto dto : page.getItems()) {
                Optional<SignEntry> opt = repo.findByLocalId(dto.getLocalId());
                if (opt.isEmpty()) {
                    repo.save(SignEntry.builder()
                            .localId(dto.getLocalId())
                            .title(dto.getTitle())
                            .videoUrl(dto.getVideoUrl())
                            .thumbnailUrl(dto.getThumbnailUrl())
                            .signDescription(dto.getSignDescription())
                            .imagesCsv(String.join(",", dto.getImages()))
                            .sourceUrl(dto.getSourceUrl())
                            .collectionDb(dto.getCollectionDb())
                            .categoryType(dto.getCategoryType())
                            .viewCount(dto.getViewCount())
                            .build());
                    inserted++;
                } else {
                    opt.get().updateFrom(dto);
                    updated++;
                }
            }

            totalFetched += page.getItems().size();
            if (totalFetched >= totalCount) break;
            pageNo++;
        }

        return new IngestResultDto(totalCount, totalFetched, inserted, updated, pageSize);
    }

    // ===== 내부 헬퍼 =====
    private SignPageDto fetchPage(int pageNo, int pageSize) {
        SignRawResponse raw = client.fetch(null, pageNo, pageSize);
        if (raw == null || raw.getResponse() == null || raw.getResponse().getHeader() == null) {
            throw new IllegalStateException("외부 API 응답 파싱 실패");
        }
        var header = raw.getResponse().getHeader();
        if (!"0000".equals(header.getResultCode())) {
            String msg = (header.getResultMsg() != null) ? header.getResultMsg() : "외부 API 오류";
            throw new IllegalStateException("KCISA API 오류: " + msg);
        }
        var body = raw.getResponse().getBody();

        List<SignRawResponse.Item> items =
                (body != null && body.getItems() != null && body.getItems().getItem() != null)
                        ? body.getItems().getItem() : List.of();

        List<SignItemDto> list = items.stream().map(this::mapToItemDto).toList();

        String pageNoStr     = (body != null) ? body.getPageNo()     : null;
        String numOfRowsStr  = (body != null) ? body.getNumOfRows()  : null;
        String totalCountStr = (body != null) ? body.getTotalCount() : null;

        return SignPageDto.builder()
                .items(list)
                .pageNo(parseIntDefault(pageNoStr, 1))
                .numOfRows(parseIntDefault(numOfRowsStr, list.size()))
                .totalCount(parseIntDefault(totalCountStr, list.size()))
                .build();
    }

    private SignPageDto toPageDto(SignRawResponse raw) {
        var body = raw.getResponse().getBody();
        var itemsRaw = (body != null && body.getItems() != null)
                ? body.getItems().getItem()
                : List.<SignRawResponse.Item>of();

        List<SignItemDto> list = itemsRaw.stream()
                .map(this::mapToItemDto)
                .toList();

        int pageNo     = parseIntDefault(body != null ? body.getPageNo() : null, 1);
        int totalCount = parseIntDefault(body != null ? body.getTotalCount() : null, list.size());

        return SignPageDto.builder()
                .items(list)
                .pageNo(pageNo)
                .numOfRows(list.size())  // 실제 개수
                .totalCount(totalCount)
                .build();
    }

    private SignItemDto mapToItemDto(SignRawResponse.Item i) {
        return SignItemDto.builder()
                .title(nvl(i.getTitle()))
                .localId(nvl(i.getLocalId()))
                .videoUrl(nvl(i.getSubDescription()))
                .thumbnailUrl(nvl(i.getImageObject()))
                .signDescription(nvl(i.getSignDescription()))
                .images(splitCsv(i.getSignImages()))
                .sourceUrl(nvl(i.getUrl()))
                .collectionDb(nvl(i.getCollectionDb()))
                .categoryType(nvl(i.getCategoryType()))
                .viewCount(parseIntOrNull(i.getViewCount()))
                .build();
    }

    private static String nvl(String s) { return (s == null) ? "" : s; }
    private static int parseIntDefault(String s, int def) {
        try { return (s == null) ? def : Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }
    private static Integer parseIntOrNull(String s) {
        try { return (s == null) ? null : Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }
    private static List<String> splitCsv(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .toList();
    }
}
