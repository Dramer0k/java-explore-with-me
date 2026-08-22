package ru.practicum.mainsrvc.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.CategoryDto;
import ru.practicum.mainsrvc.dto.NewCategoryDto;
import ru.practicum.mainsrvc.dto.UpdateCategoryDto;
import ru.practicum.mainsrvc.service.CategoryServiceImpl;

@Slf4j
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryServiceImpl categoryService;

    public AdminCategoryController(CategoryServiceImpl categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody NewCategoryDto dto) {
        log.info("Post /admin/categories {dto={}}", dto);
        return ResponseEntity.status(201).body(categoryService.createCategory(dto));
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long catId,
            @Valid @RequestBody UpdateCategoryDto dto) {
        log.info("Patch /admin/categories/{} {dto={}}", catId, dto);
        return ResponseEntity.ok(categoryService.updateCategory(catId, dto));
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long catId) {
        log.info("Delete /admin/categories/{}", catId);
        categoryService.deleteCategory(catId);
        return ResponseEntity.noContent().build();
    }
}