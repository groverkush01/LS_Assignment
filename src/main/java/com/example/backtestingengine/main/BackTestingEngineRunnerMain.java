package com.example.backtestingengine.main;

import com.example.backtestingengine.main.strategy.MultipleTradingStrategy;
import com.example.backtestingengine.main.strategy.kalman.CointegrationTradingStrategy;
import com.example.backtestingengine.main.strategy.movingaverage.MovingAverageCrossoverStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackTestingEngineRunnerMain {
    static BackTestingEngine testingEngine= new BackTestingEngine();
    public static void main(String[] args) {
        List<List<String>> tickers = new ArrayList<>();
        tickers.add(List.of("AAPL"));
        tickers.add(List.of("MSFT"));
        tickers.add(List.of("GOOG", "MSFT"));
        tickers.add(List.of("AMZN", "META"));
        List<MultipleTradingStrategy> strategies = List.of(
                MultipleTradingStrategy.of(new MovingAverageCrossoverStrategy("AAPL", 20, 60)),
                MultipleTradingStrategy.of(new MovingAverageCrossoverStrategy("MSFT", 30, 90)),
                MultipleTradingStrategy.of(new CointegrationTradingStrategy("GOOG", "MSFT")),
                MultipleTradingStrategy.of(new CointegrationTradingStrategy("AMZN", "META"))
        );

        if (tickers.size() != strategies.size()) {
            throw new IllegalArgumentException("Tickers and strategies size must match!");
        }

        ExecutorService executorService = Executors.newFixedThreadPool(tickers.size());

        // Submit tasks for each ticker-strategy pair
        for (int i = 0; i < tickers.size(); i++) {
            List<String> ticker = tickers.get(i);
            MultipleTradingStrategy strategy = strategies.get(i);

            // Submit each backtesting task to the executor service
            executorService.submit(() -> runBacktest(ticker, strategy));
        }

        // End
        executorService.shutdown();
    }

    private static void runBacktest(List<String> ticker, MultipleTradingStrategy strategy) {
        try {
            System.out.println("Starting backtest for " + ticker + " with strategy: " + strategy);
            testingEngine.BackTest(ticker, strategy);
        } catch (Exception e) {
            System.err.println("Error running backtest for " + ticker + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
