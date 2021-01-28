package com.projet3a.smartspectro;

public class Line {

    private int xBegin;
    private final int yBegin;
    private int xEnd;
    private final int yEnd;

    public Line(int x1, int y1, int x2, int y2){
        this.xBegin = x1;
        this.yBegin = y1;
        this.xEnd = x2;
        this.yEnd = y2;
    }

    public int getXBegin(){
        return this.xBegin;
    }

    public int getXEnd(){
        return this.xEnd;
    }

    public int getYBegin(){
        return this.yBegin;
    }

    public int getYEnd(){
        return this.yEnd;
    }

    /**
     * translates line on x axis
     * */
    public void translateLineOnX(int translateX){
        this.xBegin += translateX;
        this.xEnd += translateX;
    }

    public void setX(int x){
        setxBegin(x);
        setxEnd(x);
    }

    public void setxBegin(int x){
        this.xBegin = x;
    }

    public void setxEnd(int x){
        this.xEnd = x;
    }

}
