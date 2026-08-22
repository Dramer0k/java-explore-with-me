package ru.practicum.mainsrvc.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.mainsrvc.dto.CategoryDto;
import ru.practicum.mainsrvc.dto.NewCategoryDto;
import ru.practicum.mainsrvc.dto.UpdateCategoryDto;
import ru.practicum.mainsrvc.entity.Category;

import java.util.List;

public interface CategoryService {

    CategoryDto getById(Long id);

    Page<Category> getCategoriesPage(Pageable pageable);

    CategoryDto createCategory(NewCategoryDto dto);

    CategoryDto updateCategory(Long catId, UpdateCategoryDto dto);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(int from, int size);
}