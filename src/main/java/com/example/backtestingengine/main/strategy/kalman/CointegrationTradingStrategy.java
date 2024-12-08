package com.example.backtestingengine.main.strategy.kalman;

import org.apache.commons.math3.stat.StatUtils;
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

public class CointegrationTradingStrategy extends AbstractTradingStrategy {
    private static Logger log = LoggerFactory.getLogger(CointegrationTradingStrategy.class);

    boolean mReinvest = false;

    String mInstrument1, mInstrument2;
    TradingContext mContext;
    Cointegration mCoIntegration;

    DoubleSeries mAlpha;
    DoubleSeries mBeta;
    DoubleSeries mInstrument1Series;
    DoubleSeries mInstrument2Series;
    DoubleSeries mError;
    DoubleSeries mVariance;
    DoubleSeries mModel;

    Order mInstrument1Order;
    Order mInstrument2Order;

    public CointegrationTradingStrategy(String instrument1, String instrument2) {
        this(1, instrument1, instrument2);
    }

    public CointegrationTradingStrategy(double weight, String instrument1, String instrument2) {
        setWeight(weight);
        mInstrument1 = instrument1;
        mInstrument2 = instrument2;
    }

    @Override public void onStart(TradingContext context) {
        mContext = context;
        mCoIntegration = new Cointegration(1e-10, 1e-7);
        mAlpha = new DoubleSeries("alpha");
        mBeta = new DoubleSeries("beta");
        mInstrument1Series = new DoubleSeries("instrument1");
        mInstrument2Series = new DoubleSeries("instrument2");
        mError = new DoubleSeries("error");
        mVariance = new DoubleSeries("variance");
        mModel = new DoubleSeries("model");
    }

    @Override public void onTick() {
        double instrument1 = mContext.getLastPrice(mInstrument1);
        double instrument2 = mContext.getLastPrice(mInstrument2);
        double alpha = mCoIntegration.getAlpha();
        double beta = mCoIntegration.getBeta();

        mCoIntegration.step(instrument1, instrument2);
        mAlpha.add(alpha, mContext.getTime());
        mBeta.add(beta, mContext.getTime());
        mInstrument1Series.add(instrument1, mContext.getTime());
        mInstrument2Series.add(instrument2, mContext.getTime());
        mError.add(mCoIntegration.getError(), mContext.getTime());
        mVariance.add(mCoIntegration.getVariance(), mContext.getTime());

        double error = mCoIntegration.getError();

        mModel.add(beta * instrument1 + alpha, mContext.getTime());

        if (mError.size() > 30) {
            double[] lastValues = mError.reversedStream().mapToDouble(TimeSeries.Entry::getItem).limit(15).toArray();
            double sd = Math.sqrt(StatUtils.variance(lastValues));

            if (mInstrument2Order == null && Math.abs(error) > sd) {
                double value = mReinvest ? mContext.getNetValue() : mContext.getInitialFunds();
                double baseAmount = (value * getWeight() * 0.5 * Math.min(4, mContext.getLeverage())) / (instrument2 + beta * instrument1);

                if (beta > 0 && baseAmount * beta >= 1) {
                    mInstrument2Order = mContext.order(mInstrument2, error < 0, (int) baseAmount);
                    mInstrument1Order = mContext.order(mInstrument1, error > 0, (int) (baseAmount * beta));
                }
                //log.debug("Order: baseAmount={}, residual={}, sd={}, beta={}", baseAmount, residual, sd, beta);
            } else if (mInstrument2Order != null) {
                if (mInstrument2Order.isLong() && error > 0 || !mInstrument2Order.isLong() && error < 0) {
                    mContext.close(mInstrument2Order);
                    mContext.close(mInstrument1Order);

                    mInstrument2Order = null;
                    mInstrument1Order = null;
                }
            }
        }
    }

    @Override public void onEnd() {
        log.debug("Kalman filter statistics: " + Util.writeCsv(new MultipleDoubleSeries(mInstrument1Series, mInstrument2Series, mAlpha, mBeta, mError, mVariance, mModel), Arrays.asList(mInstrument1, mInstrument2)));
    }

    @Override public String toString() {
        return "CointegrationStrategy{" +
                "minstrument2='" + mInstrument2 + '\'' +
                ", minstrument1='" + mInstrument1 + '\'' +
                '}';
    }

    public DoubleSeries getAlpha() {
        return mAlpha;
    }

    public DoubleSeries getBeta() {
        return mBeta;
    }

    public DoubleSeries getXs() {
        return mInstrument1Series;
    }

    public DoubleSeries getYs() {
        return mInstrument2Series;
    }

    public DoubleSeries getError() {
        return mError;
    }

    public DoubleSeries getVariance() {
        return mVariance;
    }

    public DoubleSeries getModel() {
        return mModel;
    }
}
