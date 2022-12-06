package com.carbongators.myfootprint;

public class TipModel {
    String tipText;
    String tipType;
    int image;


    public TipModel(String tipText, String tipType, int image) {
        this.tipText = tipText;
        this.tipType = tipType;
        this.image = image;
    }

    public String getTipText() {
        return tipText;
    }

    public String getTipType() {
        return tipType;
    }

    public int getImage() {
        return image;
    }
}
