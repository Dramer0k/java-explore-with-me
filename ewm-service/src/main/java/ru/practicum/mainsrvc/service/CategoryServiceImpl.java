package ru.practicum.mainsrvc.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainsrvc.Mapper.CategoryDtoMapper;
import ru.practicum.mainsrvc.dto.CategoryDto;
import ru.practicum.mainsrvc.dto.NewCategoryDto;
import ru.practicum.mainsrvc.dto.UpdateCategoryDto;
import ru.practicum.mainsrvc.entity.Category;
import ru.practicum.mainsrvc.exception.ConflictException;
import ru.practicum.mainsrvc.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryDtoMapper categoryDtoMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryDtoMapper categoryDtoMapper, CategoryDtoMapper categoryDtoMapper1) {
        this.categoryRepository = categoryRepository;
        this.categoryDtoMapper = categoryDtoMapper1;
    }

    public Page<Category> getCategoriesPage(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public CategoryDto getById(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new org.springframework.dao.EmptyResultDataAccessException(
                        "Категория с ID " + id + " не найдена", 1));
        return categoryDtoMapper.toCategoryDto(c);
    }

    public CategoryDto createCategory(NewCategoryDto dto) {
        String name = dto.getName();
        if (name == null || name.length() < 1 || name.length() > 50) {
            throw new IllegalArgumentException("Длина имени категории должна быть от 1 до 50 символов");
        }
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория '" + dto.getName() + "' уже существует");
        }
        Category c = new Category();
        c.setName(dto.getName());
        c = categoryRepository.save(c);
        return categoryDtoMapper.toCategoryDto(c);
    }

    public CategoryDto updateCategory(Long catId, UpdateCategoryDto dto) {
        String name = dto.getName();
        if (name == null || name.length() < 1 || name.length() > 50) {
            throw new IllegalArgumentException("Длина имени категории должна быть от 1 до 50 символов");
        }
        Category c = categoryRepository.findById(catId)
                .orElseThrow(() -> new org.springframework.dao.EmptyResultDataAccessException("Категория не найдена", 1));

        if (dto.getName() != null && !dto.getName().equals(c.getName())) {
            if (categoryRepository.existsByName(dto.getName())) {
                throw new ConflictException("Категория '" + dto.getName() + "' уже существует");
            }
            c.setName(dto.getName());
        }

        c = categoryRepository.save(c);
        return categoryDtoMapper.toCategoryDto(c);
    }

    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new org.springframework.dao.EmptyResultDataAccessException("Категория не найдена", 1);
        }
        categoryRepository.deleteById(catId);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        if (from < 0 || size <= 0 || size > 1000) {
            throw new IllegalArgumentException(
                    "Некорректные параметры пагинации: from >= 0, 0 < size <= 1000");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.unsorted());
        Page<Category> page = categoryRepository.findAll(pageRequest);

        return page.getContent().stream()
                .map(categoryDtoMapper::toCategoryDto)
                .collect(Collectors.toList());
    }


}