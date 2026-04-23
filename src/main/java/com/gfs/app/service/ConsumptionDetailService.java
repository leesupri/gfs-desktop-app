package com.gfs.app.service;

import com.gfs.app.model.ConsumptionDetailRow;
import com.gfs.app.repository.ConsumptionDetailRepository;

import java.util.List;

public class ConsumptionDetailService {

    private final ConsumptionDetailRepository repository = new ConsumptionDetailRepository();

    public List<ConsumptionDetailRow> getAll(String startDate,
                                             String endDate,
                                             String invoice,
                                             String item,
                                             String warehouse) {
        return repository.findAll(startDate, endDate, invoice, item, warehouse);
    }
}