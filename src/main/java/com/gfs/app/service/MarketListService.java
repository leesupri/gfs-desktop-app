package com.gfs.app.service;

import com.gfs.app.model.MarketListRow;
import com.gfs.app.repository.MarketListRepository;

import java.util.List;

public class MarketListService {

    private final MarketListRepository repository = new MarketListRepository();

    public List<MarketListRow> getAll(String keyword,
                                      String category,
                                      String activeFilter,
                                      String salesFilter,
                                      String stockedFilter,
                                      String purchasedFilter) {

        return repository.findAll(
                keyword,
                category,
                activeFilter,
                salesFilter,
                stockedFilter,
                purchasedFilter
        );
    }

    public List<String> getCategories() {
        return repository.getCategories();
    }
}