package ru.practicum.mainsrvc.Mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainsrvc.dto.CategoryDto;
import ru.practicum.mainsrvc.entity.Category;

@Component
public class CategoryDtoMapper {

    public CategoryDto toCategoryDto(Category c) {
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        return dto;
    }
}