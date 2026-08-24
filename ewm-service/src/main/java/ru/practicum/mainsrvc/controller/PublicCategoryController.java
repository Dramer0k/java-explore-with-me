package ru.practicum.mainsrvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.CategoryDto;
import ru.practicum.mainsrvc.service.CategoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
public class PublicCategoryController {
    private final CategoryService categoryService;

    public PublicCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Get /categories {from: {}, size: {}}", from, size);

        return ResponseEntity.ok(categoryService.getCategories(from, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        log.info("Get /categories/{}}", id);
        return ResponseEntity.ok(categoryService.getById(id));
    }

}