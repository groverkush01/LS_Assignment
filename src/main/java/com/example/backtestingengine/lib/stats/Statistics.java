package com.example.backtestingengine.lib.stats;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class Statistics {
    public static double[] drawdown(double[] series) {
        double max = Double.MIN_VALUE;
        double ddPct = Double.MAX_VALUE;
        double dd = Double.MAX_VALUE;

        for (double x : series) {
            dd = Math.min(x - max, dd);
            ddPct = Math.min(x / max - 1, ddPct);
            max = Math.max(max, x);
        }

        return new double[]{dd, ddPct};
    }

    public static double sharpe(double[] dailyReturns) {
        return StatUtils.mean(dailyReturns) / Math.sqrt(StatUtils.variance(dailyReturns)) * Math.sqrt(250);
    }

    public static double[] returns(double[] series) {
        if (series.length <= 1) {
            return new double[0];
        }

        double[] returns = new double[series.length - 1];
        for (int i = 1; i < series.length; i++) {
            returns[i - 1] = series[i] / series[i - 1] - 1;
        }

        return returns;
    }

    public static double mean(double[] series) {
        return StatUtils.mean(series);
    }

    public static double stdDev(double[] series) {
        return Math.sqrt(StatUtils.variance(series));
    }

    public static double skewness(double[] series) {
        Skewness skewness = new Skewness();
        return skewness.evaluate(series);
    }

    public static double kurtosis(double[] series) {
        Kurtosis kurtosis = new Kurtosis();
        return kurtosis.evaluate(series);
    }

    public static double[] dailyReturns(double[] series) {
        return returns(series); // Reusing the `returns` method for daily returns
    }

    public static double[] regress(double[] strategyReturns, double[] marketReturns) {
        if (strategyReturns.length != marketReturns.length) {
            throw new IllegalArgumentException("Strategy returns and market returns must have the same length.");
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < strategyReturns.length; i++) {
            regression.addData(marketReturns[i], strategyReturns[i]);
        }

        double slope = regression.getSlope();
        double intercept = regression.getIntercept();
        double rSquared = regression.getRSquare();

        return new double[]{slope, intercept, rSquared}; // Beta, Alpha, and R-squared
    }
}
