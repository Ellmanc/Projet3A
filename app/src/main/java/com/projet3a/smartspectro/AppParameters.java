package com.projet3a.smartspectro;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Singleton that contains all app parameters (slope, intercept, capture zone...)
 */
public final class AppParameters {

    private static volatile AppParameters instance = null;
    private int[] captureZone;
    private double slope;
    private double intercept;
    public String button;
    private Double[] reference;
    private Double[] sample;

    private AppParameters() {
        super();
    }

    public static AppParameters getInstance() {
        if (AppParameters.instance == null) {
            //synchronized keyword prevents any multiple instantiations by several threads
            synchronized (AppParameters.class){
                if(AppParameters.instance == null){
                    AppParameters.instance = new AppParameters();
                }
            }
        }
        return AppParameters.instance;
    }

    /* Getters and setters */

    public void setSample(ArrayList<Double> r) {
        this.sample = r.toArray(new Double[0]);
    }

    public void setReference(ArrayList<Double> r) {
        this.reference = r.toArray(new Double[0]);
    }

    public void setCaptureZone(int[] array) {
        this.captureZone = array;
    }

    public void setSlope(double s) {
        this.slope = s;
    }

    public void setButton(String s) {
        this.button = s;
    }

    public void setIntercept(double i) {
        this.intercept = i;
    }

    public ArrayList<Double> getReference() {
        return new ArrayList<>(Arrays.asList(this.reference));
    }

    public ArrayList<Double> getSample() {
        return new ArrayList<>(Arrays.asList(this.sample));
    }

    public int[] getCaptureZone() {
        return this.captureZone;
    }

    public double getSlope() {
        return this.slope;
    }

    public String getButton() {
        return this.button;
    }

    public double getIntercept() {
        return this.intercept;
    }
}
