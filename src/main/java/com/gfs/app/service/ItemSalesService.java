package com.gfs.app.service;

import com.gfs.app.model.ItemSalesRow;
import com.gfs.app.repository.ItemSalesRepository;

import java.time.LocalDate;
import java.util.List;

public class ItemSalesService {
    private final ItemSalesRepository repository = new ItemSalesRepository();

    public List<ItemSalesRow> getAll(LocalDate startDate, LocalDate endDate,
                                     String itemSearch, String departmentFilter, String categoryFilter) {
        return repository.findAll(startDate, endDate, itemSearch, departmentFilter, categoryFilter);
    }

    public List<String> getDepartments() {
        return repository.getDepartments();
    }

    public List<String> getCategories(String department) {
        return repository.getCategories(department);
    }
}