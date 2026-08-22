package ru.practicum.mainsrvc.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.CompilationDto;
import ru.practicum.mainsrvc.dto.NewCompilationDto;
import ru.practicum.mainsrvc.dto.UpdateCompilationDto;
import ru.practicum.mainsrvc.service.CompilationServiceImpl;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
public class AdminCompilationController {
    private final CompilationServiceImpl compilationService;

    public AdminCompilationController(CompilationServiceImpl compilationService) {
        this.compilationService = compilationService;
    }

        @PostMapping
    public ResponseEntity<CompilationDto> createCompilation(@Valid @RequestBody NewCompilationDto dto) {
        log.info("Post /admin/compilations {dto={}}", dto);
        CompilationDto created = compilationService.createCompilation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/")
    public ResponseEntity<CompilationDto> createCompilationWithSlash(
            @Valid @RequestBody NewCompilationDto dto) {
        log.info("Post /admin/compilations/ {dto={}}", dto);
        return createCompilation(dto);
    }

    @PatchMapping("/{compId}")
    public ResponseEntity<CompilationDto> updateCompilation(
            @PathVariable Long compId,
            @Valid @RequestBody UpdateCompilationDto dto) {
        log.info("Patch /admin/compilations/{} {dto={}}", compId, dto);
        return ResponseEntity.ok(compilationService.updateCompilation(compId, dto));
    }

    @DeleteMapping("/{compId}")
    public ResponseEntity<Void> deleteCompilation(@PathVariable Long compId) {
        log.info("Delete /admin/compilations/{}", compId);
        compilationService.deleteCompilation(compId);
        return ResponseEntity.noContent().build();
    }
}