package com.alexfh.scrabblesolver.gui.tile;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class TileProvider {

    public static final TileProvider INSTANCE = new TileProvider();

    private final Map<Integer, ScrabbleTileCache> tileCacheMap = new HashMap<>();

    public BufferedImage getTile(char letter, boolean isWild, boolean isIso, boolean isHighlighted, int tileSize) {
        return this.getTileCacheForSize(tileSize).getTile(letter, isWild, isIso, isHighlighted);
    }

    public BufferedImage getWildcardTile(boolean isIso, int tileSize) {
        return this.getTileCacheForSize(tileSize).getWildcardTile(isIso);
    }

    public BufferedImage getBlankTile(int letterMultiplier, int wordMultiplier, int tileSize) {
        return this.getTileCacheForSize(tileSize).getBlankTile(letterMultiplier, wordMultiplier);
    }

    public BufferedImage getDefaultBlankTile(int tileSize) {
        return this.getTileCacheForSize(tileSize).getDefaultBlankTile();
    }

    private ScrabbleTileCache getTileCacheForSize(int tileSize) {
        ScrabbleTileCache tileCache = this.tileCacheMap.get(tileSize);

        if (tileCache == null) {
            tileCache = new ScrabbleTileCache(tileSize);

            this.tileCacheMap.put(tileSize, tileCache);
        }

        return tileCache;
    }

    public void clearCache() {
        this.tileCacheMap.clear();
    }

}
