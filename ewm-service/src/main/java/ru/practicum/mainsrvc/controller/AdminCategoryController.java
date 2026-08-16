package ru.practicum.mainsrvc.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.CategoryDto;
import ru.practicum.mainsrvc.dto.NewCategoryDto;
import ru.practicum.mainsrvc.dto.UpdateCategoryDto;
import ru.practicum.mainsrvc.service.CategoryService;

@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private static final Logger log = LoggerFactory.getLogger(AdminCategoryController.class);
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
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