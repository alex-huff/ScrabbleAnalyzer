package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.ai.PermuteTree;
import com.alexfh.scrabbleai.dictionary.WordGraphDictionary;
import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.rule.ILetterScoreMap;
import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ScrabbleGame {

    private static final boolean[] allValid = new boolean[ScrabbleUtil.alphaChars.length];
    private static final boolean[] allInvalid = new boolean[ScrabbleUtil.alphaChars.length];

    static {
        Arrays.fill(ScrabbleGame.allValid, true);
    }

    public record Move(String playedWord, char[] playedTiles, boolean isVertical, int row, int col) { }

    public class Placement {

        private final int row;
        private final int col;
        private final boolean isVertical;
        private final int minTilesPlaced;
        private final int maxTilesPlaced;
        private final int maxWordSize;
        private final char[] currentlyPlacedTiles;
        private final char[] effectiveWord;
        private final int[] posInEffectiveWordMap;
        private final int[] effectiveWordSizeMap;
        private int numPlacedTiles;
        private int currentEffectiveWordSize;
        private boolean[][] validPerpTilesForPlacement;

        public Placement(int row, int col, boolean isVertical, int minTilesPlaced, int maxTilesPlaced, char[] effectiveWord, int[] posInEffectiveWordMap, int[] effectiveWordSizeMap) {
            this.row = row;
            this.col = col;
            this.isVertical = isVertical;
            this.minTilesPlaced = minTilesPlaced;
            this.maxTilesPlaced = maxTilesPlaced;
            this.currentlyPlacedTiles = new char[this.maxTilesPlaced];
            this.effectiveWord = effectiveWord;
            this.maxWordSize = this.effectiveWord.length;
            this.posInEffectiveWordMap = posInEffectiveWordMap;
            this.effectiveWordSizeMap = effectiveWordSizeMap;
            this.validPerpTilesForPlacement = new boolean[this.maxTilesPlaced][];

            for (int i = 0; i < this.validPerpTilesForPlacement.length; i++) {
                int spotInWord = this.posInEffectiveWordMap[i];
                this.validPerpTilesForPlacement[i] = this.isVertical ?
                    ScrabbleGame.this.perpVert[row + spotInWord][col] :
                    ScrabbleGame.this.perpHori[row][col + spotInWord];
            }
        }

        public boolean placeTile(char tile) {
            return this.placeTileAs(tile, tile);
        }

        public boolean placeWildcardTileAs(char as) {
            return this.placeTileAs(ScrabbleUtil.wildCardTile, as);
        }

        private boolean placeTileAs(char tile, char as) {
            if (!this.validPerpTilesForPlacement[this.numPlacedTiles][ScrabbleUtil.charToInt(as)]) {
                return false;
            }

            this.currentlyPlacedTiles[this.numPlacedTiles] = tile;
            this.effectiveWord[this.posInEffectiveWordMap[this.numPlacedTiles]] = as;
            this.currentEffectiveWordSize = this.effectiveWordSizeMap[this.numPlacedTiles];
            this.numPlacedTiles++;

            return true;
        }

        public void removeTile() {
            this.numPlacedTiles--;
            this.currentEffectiveWordSize = this.numPlacedTiles == 0 ? 0 : this.effectiveWordSizeMap[this.numPlacedTiles - 1];
        }

    }

    private final ILetterScoreMap letterScoreMap;
    private final WordGraphDictionary dictionary;
    private final IScrabbleBoard board;
    private final char[] playerTiles;

    /*
    perpVert and perpHori are valid tile placements at a given position based on created perpendicular words

    t, f = true, false
    .    = blank tile
    *    = blank tile considered for placement of new tile
    -, | = blank tiles on the line of consideration

    --------
    perpVert
    --------
    i..|..
    t..|..
    ..b*t.
       ^
       |
         a  b  c  d  e  f  g  h  i  j  k  l  m  n  o  p  q  r  s  t  u  v  w  d1  d2  z
       [ t, f, f, f, t, f, f, f, t, f, f, f, f, f, t, f, f, f, f, f, t, f, f, f, f, f ] <- perpVert[1][3]
       since 'bat', 'bet', 'bit', 'bot', and 'but' are valid words

    --------
    perpHori
    --------

    foo...      a  b  c  d  e  f  g  h  i  j  k  l  m  n  o  p  q  r  s  t  u  v  w  d1  d2  z
    -----* <- [ f, t, t, f, f, t, f, t, f, f, f, f, t, t, f, t, f, t, t, f, f, f, f, f, f, f ] <- perpHori[0][5]
    .....a    since 'bat', 'cat', 'fat', 'hat', 'mat', 'nat', 'pat', 'rat', and 'sat' are valid words
    .....t

    perpVert and perpHori work to limit what permutations of the playerTiles are used to create words, since
    words will only be valid if they only form valid words in the perpendicular direction

    when it is being considered whether to place a playerTile at a position,
    this.perp(Vert|Hori)[row][col][ScrabbleUtil.charToInt(char)] will show if it is a valid placement perpendicularly
     */
    private final boolean[][][] perpVert;
    private final boolean[][][] perpHori;
    private final PermuteTree permuteTree;
    private final List<Placement> validPlacements;
    private final boolean[] possibleCharPlacements;
    private final boolean[][] canPlaceVert;
    private final boolean[][] canPlaceHori;

    public ScrabbleGame(ILetterScoreMap letterScoreMap, WordGraphDictionary dictionary, IScrabbleBoard board, char[] playerTiles) {
        if (dictionary.getRoot() == null) throw new IllegalStateException("Empty dictionary");

        this.letterScoreMap = letterScoreMap;
        this.dictionary = dictionary;
        this.board = board;
        this.playerTiles = playerTiles;
        this.perpVert = new boolean[this.board.getRows()][this.board.getCols()][];
        this.perpHori = new boolean[this.board.getRows()][this.board.getCols()][];
        this.possibleCharPlacements = this.getPossiblePlacements();
        this.canPlaceVert = new boolean[this.board.getRows()][this.board.getCols()];
        this.canPlaceHori = new boolean[this.board.getRows()][this.board.getCols()];
        this.permuteTree = ScrabbleUtil.timeRetrieval(() -> new PermuteTree(this.playerTiles), "generatePermuteTree");

        ScrabbleUtil.timeIt(this::initializeValidPerpendicularPlacements, "initializeValidPerpendicularPlacements");

        this.validPlacements = ScrabbleUtil.timeRetrieval(this::findValidPlacements, "findValidPlacements");
    }

    public List<Move> findMoves() {
        // this.permuteTree.forEach(System.out::println);

//        this.validPlacements.stream().sequential().filter(placement -> placement.isVertical).forEach(
//            placement -> System.out.println(
//                "Vert: " + placement.isVertical +
//                    " Row: " + placement.row +
//                    " Col: " + placement.col +
////                    " MinPlaced: " + placement.minTilesPlaced +
////                    " MaxPlaced: " + placement.maxTilesPlaced +
////                    " MaxWordSize: " + placement.maxWordSize +
//                    " EffectiveWord: " + Arrays.toString(placement.effectiveWord) +
//                    " PosInWord: " + Arrays.toString(placement.posInEffectiveWordMap) +
//                    " WordSize: " + Arrays.toString(placement.effectiveWordSizeMap)
//            )
//        );

        List<Move> ret = new ArrayList<>();

        return ret;
    }

    private boolean[] getPossiblePlacements() {
        boolean[] placements = new boolean[ScrabbleUtil.alphaChars.length];

        for (char c : this.playerTiles) {
            if (c == ScrabbleUtil.wildCardTile) {
                return ScrabbleGame.allValid;
            }

            placements[ScrabbleUtil.charToInt(c)] = true;
        }

        return placements;
    }

    private List<Placement> findValidPlacements() {
        List<Placement> placements = new LinkedList<>();

        for (int row = 0; row < this.board.getRows(); row++) {
            for (int col = 0; col < this.board.getCols(); col++) {
                Placement vert = this.getVertPlacement(row, col), hori = this.getHoriPlacement(row, col);

                if (vert != null) placements.add(vert);

                if (hori != null) placements.add(hori);
            }
        }

        return placements;
    }

    private Placement getVertPlacement(int row, int col) {
        boolean topEmpty = row == 0 || this.board.isEmptyAt(row - 1, col);

        if (!topEmpty) return null; // letter above this position

        int blanks = 0;
        int current = row;
        boolean hasAnchor = false;
        int blanksTillAnchor = 0;

        while (current < this.board.getRows()) {
            boolean isBlank = this.board.isEmptyAt(current, col);

            if (isBlank) {
                if (blanks == this.playerTiles.length || !this.canPlaceVert[current][col]) break;

                blanks++;
            }

            if (!hasAnchor) {
                boolean onLeft = col > 0 && !this.board.isEmptyAt(current, col - 1);
                boolean onRight = col < this.board.getCols() - 1 && !this.board.isEmptyAt(current, col + 1);
                boolean onAnchor = current == this.board.getAnchorRow() && col == this.board.getAnchorCol();
                boolean anchorable = !isBlank || onLeft || onRight || onAnchor;

                if (anchorable) {
                    hasAnchor = true;
                    blanksTillAnchor = blanks;
                }
            }

            current++;
        }

        if (!hasAnchor || blanks == 0) return null;

        int maxWordLength = current - row;
        char[] effectiveWord = new char[maxWordLength];
        int[] posInEffectiveWordMap = new int[blanks];
        int[] effectiveWordSizeMap = new int[blanks];
        current = row;
        int stop = row + maxWordLength;
        int i = 0;
        int b = 0;

        while (current < stop) {
            effectiveWord[i] = this.board.getCharAt(current, col);
            boolean isBlank = this.board.isEmptyAt(current, col);

            if (isBlank) {
                posInEffectiveWordMap[b] = i;
                b++;
            }

            i++;
            current++;
        }

        System.arraycopy(posInEffectiveWordMap, 1, effectiveWordSizeMap, 0, effectiveWordSizeMap.length - 1);

        effectiveWordSizeMap[effectiveWordSizeMap.length - 1] = maxWordLength;

        return new Placement(
            row,
            col,
            true,
            blanksTillAnchor,
            blanks,
            effectiveWord,
            posInEffectiveWordMap,
            effectiveWordSizeMap
        );
    }

    private Placement getHoriPlacement(int row, int col) {
        boolean leftEmpty = col == 0 || this.board.isEmptyAt(row, col - 1);

        if (!leftEmpty) return null; // letter to the left of this position

        int blanks = 0;
        int current = col;
        boolean hasAnchor = false;
        int blanksTillAnchor = 0;

        while (current < this.board.getCols()) {
            boolean isBlank = this.board.isEmptyAt(row, current);

            if (isBlank) {
                if (blanks == this.playerTiles.length || !this.canPlaceHori[row][current]) break;

                blanks++;
            }

            if (!hasAnchor) {
                boolean onTop = row > 0 && !this.board.isEmptyAt(row - 1, current);
                boolean onBot = row < this.board.getRows() - 1 && !this.board.isEmptyAt(row + 1, current);
                boolean onAnchor = current == this.board.getAnchorCol() && row == this.board.getAnchorRow();
                boolean anchorable = !isBlank || onTop || onBot || onAnchor;

                if (anchorable) {
                    hasAnchor = true;
                    blanksTillAnchor = blanks;
                }
            }

            current++;
        }

        if (!hasAnchor || blanks == 0) return null;

        int maxWordLength = current - col;
        char[] effectiveWord = new char[maxWordLength];
        int[] posInEffectiveWordMap = new int[blanks];
        int[] effectiveWordSizeMap = new int[blanks];
        current = col;
        int stop = col + maxWordLength;
        int i = 0;
        int b = 0;

        while (current < stop) {
            effectiveWord[i] = this.board.getCharAt(row, current);
            boolean isBlank = this.board.isEmptyAt(row, current);

            if (isBlank) {
                posInEffectiveWordMap[b] = i;
                b++;
            }

            i++;
            current++;
        }

        System.arraycopy(posInEffectiveWordMap, 1, effectiveWordSizeMap, 0, effectiveWordSizeMap.length - 1);

        effectiveWordSizeMap[effectiveWordSizeMap.length - 1] = maxWordLength;

        return new Placement(
            row,
            col,
            false,
            blanksTillAnchor,
            blanks,
            effectiveWord,
            posInEffectiveWordMap,
            effectiveWordSizeMap
        );
    }

    private void initializeValidPerpendicularPlacements() {
        for (int row = 0; row < this.board.getRows(); row++) {
            for (int col = 0; col < this.board.getCols(); col++) {
                if (!this.board.isEmptyAt(row, col)) { // tile already placed, nothing valid
                    this.perpVert[row][col] = ScrabbleGame.allInvalid;
                    this.perpHori[row][col] = ScrabbleGame.allInvalid;

                    continue;
                }

                boolean leftEmpty = col == 0 || this.board.isEmptyAt(row, col - 1);
                boolean rightEmpty = col == this.board.getCols() - 1 || this.board.isEmptyAt(row, col + 1);
                boolean topEmpty = row == 0 || this.board.isEmptyAt(row - 1, col);
                boolean bottomEmpty = row == this.board.getRows() - 1 || this.board.isEmptyAt(row + 1, col);

                if (leftEmpty && rightEmpty) { // nothing to left or right, anything is valid for vertical placement
                    this.perpVert[row][col] = ScrabbleGame.allValid;
                    this.canPlaceVert[row][col] = true;
                } else {
                    this.perpVert[row][col] = this.getValidReplacementsVert(row, col);
                }

                if (topEmpty && bottomEmpty) { // nothing above or below, anything is valid for horizontal placement
                    this.perpHori[row][col] = ScrabbleGame.allValid;
                    this.canPlaceHori[row][col] = true;
                } else {
                    this.perpHori[row][col] = this.getValidReplacementsHori(row, col);
                }
            }
        }
    }

    private boolean[] getValidReplacementsVert(int row, int col) {
        int left = col, right = col;

        while (left - 1 >= 0 && !this.board.isEmptyAt(row, left - 1))
            left--;

        WordGraphDictionary.WGNode currentPath = this.dictionary.getRoot();
        int current = left;

        while (current < col) {
            currentPath = currentPath.getPath(this.board.getCharAt(row, current));

            if (currentPath == null) break;

            current++;
        }

        boolean[] result;

        if (current == col) { // left of tile is valid prefix
            result = new boolean[ScrabbleUtil.alphaChars.length];

            while (right + 1 < this.board.getCols() && !this.board.isEmptyAt(row, right + 1))
                right++;

            boolean hasAny = false;

            for (Character c : currentPath.getPaths()) {
                if (!this.possibleCharPlacements[ScrabbleUtil.charToInt(c)]) continue;

                WordGraphDictionary.WGNode rightPath = currentPath.getPath(c);
                current = col + 1;

                while (current <= right) {
                    rightPath = rightPath.getPath(this.board.getCharAt(row, current));

                    if (rightPath == null) break;

                    current++;
                }

                if (rightPath != null && rightPath.wordHere) {
                    result[ScrabbleUtil.charToInt(c)] = true;
                    hasAny = true;
                }
            }

            if (hasAny) this.canPlaceVert[row][col] = true;
        } else {
            result = ScrabbleGame.allInvalid;
        }

        return result;
    }

    private boolean[] getValidReplacementsHori(int row, int col) {
        int top = row, bottom = row;

        while (top - 1 >= 0 && !this.board.isEmptyAt(top - 1, col))
            top--;

        WordGraphDictionary.WGNode currentPath = this.dictionary.getRoot();
        int current = top;

        while (current < row) {
            currentPath = currentPath.getPath(this.board.getCharAt(current, col));

            if (currentPath == null) break;

            current++;
        }

        boolean[] result;

        if (current == row) { // top of tile is valid prefix
            result = new boolean[ScrabbleUtil.alphaChars.length];

            while (bottom + 1 < this.board.getRows() && !this.board.isEmptyAt(bottom + 1, col))
                bottom++;

            boolean hasAny = false;

            for (Character c : currentPath.getPaths()) {
                if (!this.possibleCharPlacements[ScrabbleUtil.charToInt(c)]) continue;

                WordGraphDictionary.WGNode bottomPath = currentPath.getPath(c);
                current = row + 1;

                while (current <= bottom) {
                    bottomPath = bottomPath.getPath(this.board.getCharAt(current, col));

                    if (bottomPath == null) break;

                    current++;
                }

                if (bottomPath != null && bottomPath.wordHere) {
                    result[ScrabbleUtil.charToInt(c)] = true;
                    hasAny = true;
                }
            }

            if (hasAny) this.canPlaceHori[row][col] = true;
        } else {
            result = ScrabbleGame.allInvalid;
        }

        return result;
    }

}
