package com.alexfh.scrabblesolver.gui.tile;

import com.alexfh.scrabblesolver.gui.svg.BufferedImageTranscoder;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import java.awt.image.BufferedImage;

public class ScrabbleTileCache {

    private final int tileSize;
    private final BufferedImage[][][][] letterCache = new BufferedImage[IScrabbleGameState.alphaChars.length][2][2][2];
    private final BufferedImage[] wildcardCache = new BufferedImage[2];
    private final BufferedImage[][] blankTileCache = new BufferedImage[2][2];
    private BufferedImage defaultBlankTile;

    public ScrabbleTileCache(int tileSize) {
        this.tileSize = tileSize;
    }

    public BufferedImage getTile(char letter, boolean isWild, boolean isIso, boolean isHighlighted) {
        int letterIndex = ScrabbleUtil.charToInt(letter);
        int wildIndex = isWild ? 1 : 0;
        int isoIndex = isIso ? 1 : 0;
        int highlightedIndex = isHighlighted ? 1 : 0;
        BufferedImage tile = this.letterCache[letterIndex][wildIndex][isoIndex][highlightedIndex];

        if (tile == null) {
            tile = BufferedImageTranscoder.INSTANCE.loadImage(
                DocumentProvider.INSTANCE.getTileDocument(
                    letter,
                    isWild,
                    isIso,
                    isHighlighted
                ),
                this.tileSize,
                this.tileSize
            );
            this.letterCache[letterIndex][wildIndex][isoIndex][highlightedIndex] = tile;
        }

        return tile;
    }

    public BufferedImage getWildcardTile(boolean isIso) {
        int index = isIso ? 1 : 0;
        BufferedImage tile = this.wildcardCache[index];

        if (tile == null) {
            tile = BufferedImageTranscoder.INSTANCE.loadImage(
                DocumentProvider.INSTANCE.getWildcardDocument(isIso),
                this.tileSize,
                this.tileSize
            );
            this.wildcardCache[index] = tile;
        }

        return tile;
    }

    public BufferedImage getBlankTile(int letterMultiplier, int wordMultiplier) {
        if (letterMultiplier == 1 && wordMultiplier == 1) return this.getDefaultBlankTile();

        if (letterMultiplier > 1 && letterMultiplier < 4) {
            BufferedImage tile = this.blankTileCache[0][letterMultiplier - 2];

            if (tile == null) {
                tile = BufferedImageTranscoder.INSTANCE.loadImage(
                    DocumentProvider.INSTANCE.getBlankTileDocument(letterMultiplier, wordMultiplier),
                    this.tileSize,
                    this.tileSize
                );
                this.blankTileCache[0][letterMultiplier - 2] = tile;
            }

            return tile;
        } else if (wordMultiplier > 1 && wordMultiplier < 4) {
            BufferedImage tile = this.blankTileCache[1][wordMultiplier - 2];

            if (tile == null) {
                tile = BufferedImageTranscoder.INSTANCE.loadImage(
                    DocumentProvider.INSTANCE.getBlankTileDocument(letterMultiplier, wordMultiplier),
                    this.tileSize,
                    this.tileSize
                );
                this.blankTileCache[1][wordMultiplier - 2] = tile;
            }

            return tile;
        } else {
            return null;
        }
    }

    public BufferedImage getDefaultBlankTile() {
        BufferedImage tile = this.defaultBlankTile;

        if (tile == null) {
            tile = BufferedImageTranscoder.INSTANCE.loadImage(
                DocumentProvider.INSTANCE.getDefaultBlankDocument(),
                this.tileSize,
                this.tileSize
            );
            this.defaultBlankTile = tile;
        }

        return tile;
    }

}
