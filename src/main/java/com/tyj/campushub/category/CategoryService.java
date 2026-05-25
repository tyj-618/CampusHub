package com.tyj.campushub.category;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listEnabledCategories() {
        return categoryRepository.findEnabledCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
