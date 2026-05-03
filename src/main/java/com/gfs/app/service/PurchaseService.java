package com.gfs.app.service;

import com.gfs.app.model.PurchaseDetailRow;
import com.gfs.app.model.PurchaseOrderSummaryRow;
import com.gfs.app.model.PurchaseSummaryRow;
import com.gfs.app.repository.PurchaseRepository;

import java.time.LocalDate;
import java.util.List;

public class PurchaseService {

    private final PurchaseRepository repository = new PurchaseRepository();

    // Purchase Summary (v_purchase_card)
    public List<PurchaseSummaryRow> getSummary(LocalDate startDate, LocalDate endDate,
                                                String categoryFilter, String itemFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findSummary(startDate, endDate, categoryFilter, itemFilter);
    }

    // Purchase Detail grouped by Category
    public List<PurchaseDetailRow> getDetailByCategory(LocalDate startDate, LocalDate endDate,
                                                        String categoryFilter, String partnerFilter,
                                                        String warehouseFilter, String itemFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findDetail(startDate, endDate,
            categoryFilter, partnerFilter, warehouseFilter, itemFilter, false);
    }

    // Purchase Detail grouped by Partner
    public List<PurchaseDetailRow> getDetailByPartner(LocalDate startDate, LocalDate endDate,
                                                       String categoryFilter, String partnerFilter,
                                                       String warehouseFilter, String itemFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findDetail(startDate, endDate,
            categoryFilter, partnerFilter, warehouseFilter, itemFilter, true);
    }

    // Purchase Order Summary (no date filter)
    public List<PurchaseOrderSummaryRow> getOrderSummary(String categoryFilter, String itemFilter) {
        return repository.findOrderSummary(categoryFilter, itemFilter);
    }

    // Dropdowns
    public List<String> getCategories()  { return repository.findDistinctCategories(); }
    public List<String> getPartners()    { return repository.findDistinctPartners(); }
    public List<String> getWarehouses()  { return repository.findDistinctWarehouses(); }
}