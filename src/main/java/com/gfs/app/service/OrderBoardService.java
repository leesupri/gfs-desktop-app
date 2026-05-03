package com.gfs.app.service;

import com.gfs.app.model.OrderBillSummary;
import com.gfs.app.model.OrderLineRow;
import com.gfs.app.repository.OrderBoardRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mirrors the in-memory groupBy logic in OrderBoardController.php:
 *
 *   $byBill = $rows->groupBy('invoice_id')->map(function ($items, $invoiceId) {
 *       $gross    = $items->sum(qty * unitPrice)
 *       $discount = $items->sum(discountAmount)
 *       ordered_at = $items->min('created')
 *       ...
 *   });
 */
public class OrderBoardService {

    private final OrderBoardRepository repository = new OrderBoardRepository();

    /**
     * Returns raw lines — used when the caller wants to drill into a single invoice.
     */
    public List<OrderLineRow> getLines(LocalDate startDate, LocalDate endDate,
                                       String invoice, String table, String station,
                                       String department, String category, String q) {
        return repository.findAll(
            def(startDate), def(endDate),
            safe(invoice), safe(table), safe(station),
            safe(department), safe(category), safe(q)
        );
    }

    /**
     * Returns bills grouped by invoice_id — mirrors PHP $byBill.
     * Order preserved: date DESC, invoice_id DESC (as returned by the DB).
     */
    public List<OrderBillSummary> getBills(LocalDate startDate, LocalDate endDate,
                                           String invoice, String table, String station,
                                           String department, String category, String q) {
        List<OrderLineRow> lines = getLines(
            startDate, endDate, invoice, table, station, department, category, q);

        // Group preserving DB order (LinkedHashMap keeps insertion order)
        Map<Long, List<OrderLineRow>> grouped = lines.stream()
            .collect(Collectors.groupingBy(
                OrderLineRow::getInvoiceId,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<OrderBillSummary> bills = new ArrayList<>();
        for (Map.Entry<Long, List<OrderLineRow>> entry : grouped.entrySet()) {
            long             invoiceId = entry.getKey();
            List<OrderLineRow> items  = entry.getValue();
            OrderLineRow       first  = items.get(0);

            double gross    = items.stream().mapToDouble(OrderLineRow::getLineTotal).sum();
            double discount = items.stream().mapToDouble(OrderLineRow::getDiscountAmount).sum();

            // ordered_at = min(created) — same as PHP $items->min('created')
            LocalDateTime orderedAt = items.stream()
                .map(OrderLineRow::getCreated)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

            bills.add(new OrderBillSummary(
                invoiceId,
                first.getDate(),
                first.getSalesType(),
                first.getTableName(),
                first.getCustomer(),
                first.getEmployee(),      // ordered_by
                orderedAt,
                first.getCreatedAtHo(),   // order_station
                first.getClosedBy(),
                first.getClosedTime(),
                first.getClosedAt(),      // close_station
                gross,
                discount,
                items.size(),
                items
            ));
        }
        return bills;
    }

    public List<String> getDepartments()  { return repository.findDistinct("department"); }
    public List<String> getCategories()   { return repository.findDistinct("category"); }
    public List<String> getStations()     { return repository.findDistinct("createdAtHo"); }
    public List<String> getSalesTypes()   { return repository.findDistinct("salesType"); }

    private static LocalDate      def(LocalDate d)  { return d != null ? d : LocalDate.now(); }
    private static String         safe(String s)    { return s != null ? s.trim() : ""; }
}