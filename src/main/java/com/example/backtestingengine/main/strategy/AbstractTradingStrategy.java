package com.example.backtestingengine.main.strategy;

import com.example.backtestingengine.lib.model.TradingStrategy;

public abstract class AbstractTradingStrategy implements TradingStrategy {
    double mWeight = 1;

    public double getWeight() {
        return mWeight;
    }

    public void setWeight(double weight) {
        mWeight = weight;
    }
}
