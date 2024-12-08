package com.example.backtestingengine.main;

import com.example.backtestingengine.main.backtest.Backtest;
import com.example.backtestingengine.lib.model.ClosedOrder;
import com.example.backtestingengine.lib.series.DoubleSeries;
import com.example.backtestingengine.lib.series.MultipleDoubleSeries;
import com.example.backtestingengine.lib.api.AlphaVantageHistoricalPriceService;
import com.example.backtestingengine.lib.api.HistoricalPriceService;
import com.example.backtestingengine.lib.stats.Statistics;
import com.example.backtestingengine.lib.csv.Util;
import com.example.backtestingengine.main.strategy.MultipleTradingStrategy;

import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

public class BackTestingEngine {
    static String alphaVantantageApiKey = ""; // fill API key in here

    public void BackTest(List<String> Tickers, MultipleTradingStrategy tradingStrategy){
        checkApiKey();

        HistoricalPriceService finance = new AlphaVantageHistoricalPriceService(alphaVantantageApiKey); // Initializing the PriceService

        MultipleDoubleSeries priceSeries = new MultipleDoubleSeries(
                Tickers.stream()
                        .map(ticker -> finance.getHistoricalAdjustedPrices(ticker).blockingFirst()) // Fetch prices for each ticker
                        .toArray(DoubleSeries[]::new)); // Prices or every ticker in the trading strategy

        int deposit = 15000; // initial deposit same for all equities
        Backtest backtest = new Backtest(deposit, priceSeries);
        backtest.setLeverage(4);

        // do the backtest
        Backtest.Result result = backtest.run(tradingStrategy);

        StringBuilder orders = new StringBuilder();
        orders.append("id,amount,side,instrument,from,to,open,close,pl\n");
        for (ClosedOrder order : result.getOrders()) {
            orders.append(format(Locale.US, "%d,%d,%s,%s,%s,%s,%f,%f,%f\n", order.getId(), Math.abs(order.getAmount()), order.isLong() ? "Buy" : "Sell", order.getInstrument(), order.getOpenInstant(), order.getCloseInstant(), order.getOpenPrice(), order.getClosePrice(), order.getPl()));
        }

        int days = priceSeries.size();
        StringBuilder results = new StringBuilder();
        results.append(String.format(Locale.US, "Simulated %d days, Initial deposit %d, Leverage %f%n", days, deposit, backtest.getLeverage()))
                .append(String.format(Locale.US, "Commissions = %.2f%n", result.getCommissions()))
                .append(String.format(Locale.US, "P/L = %.2f, Final value = %.2f, Result = %.2f%%, Annualized = %.2f%%, Sharpe (rf=0%%) = %.2f%n", result.getPl(), result.getFinalValue(), result.getReturn() * 100, result.getReturn() / (days / 251.) * 100, result.getSharpe()));

        // Analyze returns
        double[] dailyReturns = Statistics.dailyReturns(result.getAccountValueHistory().toArray());
        StringBuilder stats = new StringBuilder();
        stats.append(String.format(Locale.US, "Average Return = %.6f%n", Statistics.mean(dailyReturns)))
                .append(String.format(Locale.US, "Return Volatility = %.6f%n", Statistics.stdDev(dailyReturns)))
                .append(String.format(Locale.US, "Skewness = %.6f%n", Statistics.skewness(dailyReturns)))
                .append(String.format(Locale.US, "Kurtosis = %.6f%n", Statistics.kurtosis(dailyReturns)))
                .append(String.format(Locale.US, "Max Drawdown = %.6f%n", result.getMaxDrawdown()))
                .append(String.format(Locale.US, "Max Drawdown Percentage = %.2f%%%n", result.getMaxDrawdownPercent() * 100))
                .append(String.format(Locale.US, "Signal Accuracy = %.2f%%%n", result.getSignalAccuracy() * 100));


        System.out.println();
        System.out.print("Backtest result of " + tradingStrategy.getClass() + ": " + tradingStrategy + "\nTickers: " + Tickers + "\nPrices: " + priceSeries + "\n" + results);
        System.out.println("Orders for tickers: " + Tickers + "- " + Util.writeStringToTempFile(orders.toString(), Tickers));
        System.out.println("P/L for tickers: " + Tickers + "- " + Util.writeCsv(new MultipleDoubleSeries(result.getPlHistory(), result.getMarginHistory()), Tickers));
        System.out.println("Statistics of " + tradingStrategy.getClass() + ": " + tradingStrategy + "\nTickers: " + Tickers + "\nPrices: " + priceSeries + "\n" + stats);

    }
    private static void checkApiKey() {
        if (alphaVantantageApiKey.isEmpty()) {
            System.out.println("ERROR: Get alphavantage API key at https://www.alphavantage.co/support/#api-key and set the alphaVantantageApiKey in main package file BackTestingEngine.java");
            System.exit(1);
        }
    }
}