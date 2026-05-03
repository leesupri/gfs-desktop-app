package com.gfs.app.service;

import com.gfs.app.model.NoSalesItemRow;
import com.gfs.app.model.NoSalesReceiptRow;
import com.gfs.app.repository.NoSalesRepository;

import java.time.LocalDate;
import java.util.List;

public class NoSalesService {

    private final NoSalesRepository repository = new NoSalesRepository();

    public List<NoSalesItemRow> getItemSummary(LocalDate startDate, LocalDate endDate,
                                                String departmentFilter, String categoryFilter,
                                                String descriptionFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findItemSummary(startDate, endDate,
            departmentFilter, categoryFilter, descriptionFilter);
    }

    public List<NoSalesReceiptRow> getReceiptDetail(LocalDate startDate, LocalDate endDate,
                                                     String tableFilter, String staffFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findReceiptDetail(startDate, endDate, tableFilter, staffFilter);
    }

    public List<String> getDepartments() { return repository.findDistinctDepartments(); }
    public List<String> getCategories()  { return repository.findDistinctCategories(); }
}