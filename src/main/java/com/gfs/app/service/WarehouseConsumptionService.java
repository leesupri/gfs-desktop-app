package com.gfs.app.service;

import com.gfs.app.model.WarehouseConsumptionRow;
import com.gfs.app.repository.WarehouseConsumptionRepository;

import java.time.LocalDate;
import java.util.List;

public class WarehouseConsumptionService {
    private final WarehouseConsumptionRepository repository = new WarehouseConsumptionRepository();

    public List<WarehouseConsumptionRow> getAll(LocalDate startDate, LocalDate endDate, String warehouseFilter) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        return repository.findAll(startDate, endDate, warehouseFilter);
    }
}