package com.gfs.app.service;

import com.gfs.app.model.ReceiptDetailRow;
import com.gfs.app.model.ReceiptSummaryRow;
import com.gfs.app.repository.ReceiptRepository;

import java.time.LocalDate;
import java.util.List;

public class ReceiptService {

    private final ReceiptRepository repository = new ReceiptRepository();

    public List<ReceiptSummaryRow> getSummary(LocalDate startDate, LocalDate endDate,
                                               String invoiceFilter, String typeFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findSummary(startDate, endDate, invoiceFilter, typeFilter);
    }

    public List<ReceiptDetailRow> getDetail(long invoiceId) {
        return repository.findDetail(invoiceId);
    }

    public List<String> getTypes() {
        return repository.findDistinctTypes();
    }
}