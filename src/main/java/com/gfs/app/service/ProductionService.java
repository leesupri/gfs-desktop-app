package com.gfs.app.service;

import com.gfs.app.model.ProductionDetailRow;
import com.gfs.app.model.ProductionSummaryRow;
import com.gfs.app.repository.ProductionRepository;
import com.gfs.app.repository.ProductionRepository.ProductionHeaderRow;

import java.time.LocalDate;
import java.util.List;

public class ProductionService {

    private final ProductionRepository repository = new ProductionRepository();

    // Summary — grouped aggregate from v_production_card
    public List<ProductionSummaryRow> getSummary(LocalDate startDate, LocalDate endDate,
                                                  String categoryFilter, String warehouseFilter,
                                                  String itemFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findSummary(startDate, endDate, categoryFilter, warehouseFilter, itemFilter);
    }

    // Production ID list — used to populate the "click to view detail" list
    public List<ProductionHeaderRow> getHeaders(LocalDate startDate, LocalDate endDate,
                                                 String warehouseFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findHeaders(startDate, endDate, warehouseFilter);
    }

    // Detail — full ingredient breakdown for one production ID
    public List<ProductionDetailRow> getDetail(long productionId) {
        return repository.findDetail(productionId);
    }

    public List<String> getWarehouses()  { return repository.findDistinctWarehouses(); }
    public List<String> getCategories()  { return repository.findDistinctCategories(); }
}