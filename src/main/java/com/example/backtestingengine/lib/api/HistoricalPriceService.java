package com.example.backtestingengine.lib.api;

import com.example.backtestingengine.lib.series.DoubleSeries;
import io.reactivex.rxjava3.core.Observable;

public interface HistoricalPriceService {
    Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol);
}
