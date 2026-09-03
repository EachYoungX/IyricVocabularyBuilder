package com.each17.backend.song.service;

import com.each17.backend.dto.SongCreditDto;
import com.each17.backend.lyric.entity.LyricLine;
import com.each17.backend.lyric.entity.LyricLineType;
import com.each17.backend.lyric.service.CreditLineClassifier;
import com.each17.backend.song.entity.Song;
import com.each17.backend.song.entity.SongCredit;
import com.each17.backend.song.repository.SongCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SongCreditService {
    private final SongCreditRepository repository;
    private final CreditLineClassifier classifier;

    public void replaceCredits(Song song, List<LyricLine> lines) {
        repository.deleteBySongId(song.getId());
        List<SongCredit> credits = new ArrayList<>();
        int sortOrder = 0;
        for (LyricLine line : lines) {
            if (line.getLineType() != LyricLineType.CREDIT) continue;
            var parsed = classifier.parse(line.getNormalizedText());
            if (parsed.isEmpty()) continue;
            credits.add(SongCredit.builder()
                    .song(song)
                    .creditType(parsed.get().creditType())
                    .creditLabel(parsed.get().creditLabel())
                    .creditValue(parsed.get().creditValue())
                    .sourceLineId(line.getId())
                    .sortOrder(sortOrder++)
                    .build());
        }
        if (!credits.isEmpty()) repository.saveAll(credits);
    }

    @Transactional(readOnly = true)
    public List<SongCreditDto> findDtos(Long songId) {
        return repository.findBySongIdOrderBySortOrderAscIdAsc(songId).stream().map(this::toDto).toList();
    }

    public void deleteForSong(Long songId) {
        repository.deleteBySongId(songId);
    }

    private SongCreditDto toDto(SongCredit credit) {
        return SongCreditDto.builder()
                .id(credit.getId())
                .creditType(credit.getCreditType())
                .creditLabel(credit.getCreditLabel())
                .creditValue(credit.getCreditValue())
                .sourceLineId(credit.getSourceLineId())
                .sortOrder(credit.getSortOrder())
                .build();
    }
}
