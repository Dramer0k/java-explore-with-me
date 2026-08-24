package ru.practicum.mainsrvc.service;

import ru.practicum.mainsrvc.dto.CompilationDto;
import ru.practicum.mainsrvc.dto.NewCompilationDto;
import ru.practicum.mainsrvc.dto.UpdateCompilationDto;
import ru.practicum.mainsrvc.entity.Compilation;

import java.util.List;
import java.util.Map;

public interface CompilationService {

    CompilationDto createCompilation(NewCompilationDto dto);

    List<CompilationDto> getPublicCompilations(Boolean pinned, int from, int size);

    CompilationDto getCompilationById(Long id);

    CompilationDto updateCompilation(Long compId, UpdateCompilationDto dto);

    void deleteCompilation(Long compId);

    Map<String, Long> getStatsForUris(List<String> uris);

    Map<Long, Map<String, Long>> collectStatsForCompilations(List<Compilation> compilations);

    Map<String, Long> getStatsForCompilation(Compilation compilation);
}