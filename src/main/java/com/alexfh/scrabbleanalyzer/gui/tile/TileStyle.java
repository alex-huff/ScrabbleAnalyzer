package com.alexfh.scrabbleanalyzer.gui.tile;

public
class TileStyle
{

    public static final TileStyle INSTANCE = new TileStyle(true);

    private boolean isIso;

    private
    TileStyle(boolean isIso)
    {
        this.isIso = isIso;
    }

    public
    void setIso(boolean isIso)
    {
        this.isIso = isIso;
    }

    public
    boolean getIso()
    {
        return this.isIso;
    }

}
