package com.example.backtestingengine.lib.model;

public interface TradingStrategy {
    default void onStart(TradingContext context) {

    }

    default void onTick() {

    }

    default void onEnd() {

    }
}
