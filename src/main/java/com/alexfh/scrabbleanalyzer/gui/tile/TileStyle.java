package com.alexfh.scrabbleanalyzer.gui.tile;

public class TileStyle {

    private boolean isIso;

    public TileStyle(boolean isIso) {
        this.isIso = isIso;
    }

    public void setIso(boolean isIso) {
        this.isIso = isIso;
    }

    public boolean getIso() {
        return this.isIso;
    }

}
