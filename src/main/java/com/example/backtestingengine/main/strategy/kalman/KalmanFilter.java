package com.example.backtestingengine.main.strategy.kalman;


import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.matrix.DenseMatrix;


public class KalmanFilter {
    private final int mStateCount; // n
    private final int mSensorCount; // m
    // state

    private Matrix mState; // x, state estimate

    private Matrix mStateCovariance; // Covariance matrix of x, process noise (w)

    // predict
    private Matrix mUpdateMatrix; // F, State transition matrix.

    private Matrix mUpdateCovariance; // Q, Estimated error in process.
    private Matrix mMoveVector; // u, Control vector


    // measurement
    private Matrix mMeasurement;

    private Matrix mMeasurementCovariance; // R, Covariance matrix of the measurement vector z

    private Matrix mExtractionMatrix; // H, Observation matrix.

    // no inputs
    private Matrix mInnovation;
    private Matrix mInnovationCovariance;

    public KalmanFilter(int stateCount, int sensorCount) {
        mStateCount = stateCount;
        mSensorCount = sensorCount;
        mMoveVector = Matrix.zero(stateCount, 1);
    }

    private void step() {
        // prediction
        Matrix predictedState = mUpdateMatrix.multiply(mState).add(mMoveVector);
        Matrix predictedStateCovariance = mUpdateMatrix.multiply(mStateCovariance).multiply(mUpdateMatrix.transpose()).add(mUpdateCovariance);

        // observation
        mInnovation = mMeasurement.subtract(mExtractionMatrix.multiply(predictedState));
        mInnovationCovariance = mExtractionMatrix.multiply(predictedStateCovariance).multiply(mExtractionMatrix.transpose()).add(mMeasurementCovariance);

        // update
        Matrix kalmanGain = predictedStateCovariance.multiply(mExtractionMatrix.transpose()).multiply(mInnovationCovariance.withInverter(LinearAlgebra.InverterFactory.SMART).inverse());
        mState = predictedState.add(kalmanGain.multiply(mInnovation));

        int nRow = mStateCovariance.rows();
        mStateCovariance = DenseMatrix.identity(nRow).subtract(kalmanGain.multiply(mExtractionMatrix)).multiply(predictedStateCovariance);
    }

    public void step(Matrix measurement, Matrix move) {
        mMeasurement = measurement;
        mMoveVector = move;
        step();
    }

    public void step(Matrix measurement) {
        mMeasurement = measurement;
        step();
    }


    public Matrix getState() {
        return mState;
    }

    public Matrix getStateCovariance() {
        return mStateCovariance;
    }

    public Matrix getInnovation() {
        return mInnovation;
    }

    public Matrix getInnovationCovariance() {
        return mInnovationCovariance;
    }

    public void setState(Matrix state) {
        mState = state;
    }

    public void setStateCovariance(Matrix stateCovariance) {
        mStateCovariance = stateCovariance;
    }

    public void setUpdateMatrix(Matrix updateMatrix) {
        mUpdateMatrix = updateMatrix;
    }

    public void setUpdateCovariance(Matrix updateCovariance) {
        mUpdateCovariance = updateCovariance;
    }

    public void setMeasurementCovariance(Matrix measurementCovariance) {
        mMeasurementCovariance = measurementCovariance;
    }

    public void setExtractionMatrix(Matrix h) {
        this.mExtractionMatrix = h;
    }

    public Matrix getUpdateMatrix() {
        return mUpdateMatrix;
    }

    public Matrix getUpdateCovariance() {
        return mUpdateCovariance;
    }

    public Matrix getMeasurementCovariance() {
        return mMeasurementCovariance;
    }

    public Matrix getExtractionMatrix() {
        return mExtractionMatrix;
    }
}
