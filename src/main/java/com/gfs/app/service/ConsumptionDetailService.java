package com.gfs.app.service;

import com.gfs.app.model.ConsumptionDetailRow;
import com.gfs.app.repository.ConsumptionDetailRepository;

import java.time.LocalDate;
import java.util.List;

public class ConsumptionDetailService {

    private final ConsumptionDetailRepository repository = new ConsumptionDetailRepository();

    public List<ConsumptionDetailRow> getAll(LocalDate startDate,
                                             LocalDate endDate,
                                             String invoice,
                                             String item,
                                             String warehouse) {
        // Ensure dates are not null – default to today if missing
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = LocalDate.now();

        return repository.findAll(startDate, endDate, invoice, item, warehouse);
    }
}