package com.gfs.app.service;

import com.gfs.app.model.RecipeRow;
import com.gfs.app.repository.RecipeRepository;

import java.util.List;

public class RecipeService {
    private final RecipeRepository repository = new RecipeRepository();

    public List<RecipeRow> getAll(String searchKeyword, String salesFilter,
                              String purchasedFilter, String stockedFilter, String activeFilter) {
    return repository.findAll(searchKeyword, salesFilter, purchasedFilter, stockedFilter, activeFilter);
}
}