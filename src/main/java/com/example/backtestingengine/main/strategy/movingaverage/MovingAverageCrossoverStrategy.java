package com.example.backtestingengine.main.strategy.movingaverage;

import com.example.backtestingengine.lib.model.Order;
import com.example.backtestingengine.lib.model.TradingContext;
import com.example.backtestingengine.lib.series.DoubleSeries;
import com.example.backtestingengine.lib.series.MultipleDoubleSeries;
import com.example.backtestingengine.lib.series.TimeSeries;
import com.example.backtestingengine.lib.csv.Util;
import com.example.backtestingengine.main.strategy.AbstractTradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MovingAverageCrossoverStrategy extends AbstractTradingStrategy {
    private static Logger log = LoggerFactory.getLogger(MovingAverageCrossoverStrategy.class);

    private final String mInstrument;
    TradingContext mContext;
    private final int mShortWindow;
    private final int mLongWindow;

    private DoubleSeries mShortMA;
    private DoubleSeries mLongMA;

    private Order mCurrentOrder;

    public MovingAverageCrossoverStrategy(String instrument, int shortWindow, int longWindow) {
        mInstrument = instrument;
        mShortWindow = shortWindow;
        mLongWindow = longWindow;
    }

    @Override
    public void onStart(TradingContext context) {
        mContext = context;
        mShortMA = new DoubleSeries("Short Moving Average");
        mLongMA = new DoubleSeries("Long Moving Average");
    }

    @Override
    public void onTick() {
        double price = mContext.getLastPrice(mInstrument);

        // Update moving averages
        mShortMA.add(calculateMovingAverage(mShortWindow), mContext.getTime());
        mLongMA.add(calculateMovingAverage(mLongWindow), mContext.getTime());

        // Trading logic
        if (mShortMA.size() >= mLongWindow) {
            double shortMA = mShortMA.getLast();
            double longMA = mLongMA.getLast();

            if (mCurrentOrder == null && shortMA > longMA) {
                // Buy signal
                double amount = (mContext.getAvailableFunds() * getWeight()) / price;
                mCurrentOrder = mContext.order(mInstrument, true, (int) amount);
                log.debug("Buy order placed: Amount={}, Price={}", amount, price);
            } else if (mCurrentOrder != null && shortMA < longMA) {
                // Sell signal
                mContext.close(mCurrentOrder);
                mCurrentOrder = null;
                log.debug("Sell order placed. Closing previous position.");
            }
        }
    }

    @Override
    public void onEnd() {
        log.debug("Strategy ended: Saving moving averages" + Util.writeCsv(new MultipleDoubleSeries(mShortMA, mLongMA), Arrays.asList(mInstrument)));
    }

    @Override
    public String toString() {
        return "MovingAverageCrossoverStrategy{" +
                "Instrument='" + mInstrument + '\'' +
                ", ShortWindow=" + mShortWindow +
                ", LongWindow=" + mLongWindow +
                '}';
    }

    private double calculateMovingAverage(int window) {
        List<Double> recentPrices = mContext.getHistory(mInstrument)
                .limit(window) // Take the last `window` entries
                .map(TimeSeries.Entry::getItem) // Extract price values
                .collect(Collectors.toList());

        // Ensure there are enough data points
        if (recentPrices.size() < window) {
            return 0.0;
        }

        // Calculate the average
        return recentPrices.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

    }
}

