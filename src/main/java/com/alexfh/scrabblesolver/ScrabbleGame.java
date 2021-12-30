package com.alexfh.scrabblesolver;

import com.alexfh.scrabblesolver.ai.PermuteTree;
import com.alexfh.scrabblesolver.dictionary.WordGraphDictionary;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.rule.ILetterScoreMap;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScrabbleGame {

    public static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final boolean[] allValid = new boolean[ScrabbleUtil.alphaChars.length];
    private static final boolean[] allInvalid = new boolean[ScrabbleUtil.alphaChars.length];
    private static final int invalidPerpWordScore = -1;
    private static final Offset vertOffset = new Offset(1, 0);
    private static final Offset horiOffset = new Offset(0, 1);

    static {
        Arrays.fill(ScrabbleGame.allValid, true);
    }

    private static record Offset(int moveRowBy, int moveColBy) {

        int newRow(int row, int offset) {
            return row + this.moveRowBy * offset;
        }

        int newCol(int col, int offset) {
            return col + this.moveColBy * offset;
        }

    }

    public record Move(String playedWord, char[] playedTiles, boolean isVertical, int row, int col, int score) implements Comparable<Move> {

        private static final Comparator<Move> moveComparator = Comparator.comparingInt(
            Move::score
        ).reversed().thenComparingInt(
            move -> move.playedTiles.length
        ).thenComparing(
            Move::playedWord
        ).thenComparingInt(
            Move::row
        ).thenComparingInt(
            Move::col
        );

        @Override
        public int compareTo(Move o) {
            return moveComparator.compare(this, o);
        }

    }

    private class WordStart {

        private final int row;
        private final int col;
        private final boolean isVertical;
        private final int minTilesPlaced;
        private final int maxTilesPlaced;
        private final char[] currentlyPlacedTiles;
        private final char[] effectiveWord;
        private final int[] posInEffectiveWordMap;
        private final int[] effectiveWordSizeMap;
        private int numPlacedTiles;
        private final boolean[][] validPerpTilesForPlacement;
        private final int[][] perpScoreDataSource;
        private final Offset offset;

        private WordStart(int row, int col, boolean isVertical, int minTilesPlaced, int maxTilesPlaced, char[] effectiveWord, int[] posInEffectiveWordMap, int[] effectiveWordSizeMap) {
            this.row = row;
            this.col = col;
            this.isVertical = isVertical;
            this.minTilesPlaced = minTilesPlaced;
            this.maxTilesPlaced = maxTilesPlaced;
            this.currentlyPlacedTiles = new char[this.maxTilesPlaced];
            this.effectiveWord = effectiveWord;
            this.posInEffectiveWordMap = posInEffectiveWordMap;
            this.effectiveWordSizeMap = effectiveWordSizeMap;
            this.validPerpTilesForPlacement = new boolean[this.maxTilesPlaced][];
            this.perpScoreDataSource = this.isVertical ? ScrabbleGame.this.perpScoreDataVert : ScrabbleGame.this.perpScoreDataHori;
            this.offset = this.isVertical ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;

            for (int i = 0; i < this.validPerpTilesForPlacement.length; i++) {
                int spotInWord = this.posInEffectiveWordMap[i];
                this.validPerpTilesForPlacement[i] = this.isVertical ?
                    ScrabbleGame.this.perpVert[row + spotInWord][col] :
                    ScrabbleGame.this.perpHori[row][col + spotInWord];
            }
        }

        private int getScore() {
            int playScore = 0;
            int mainWordScore = 0;
            int mainWordMultiplier = 1;
            int currentTile = 0;

            for (int i = 0; i < this.effectiveWordSizeMap[this.numPlacedTiles - 1]; i++) {
                int perpWordScore = 0;
                int newRow = this.offset.newRow(this.row, i);
                int newCol = this.offset.newCol(this.col, i);

                if (ScrabbleGame.this.board.isEmptyAt(newRow, newCol)) {
                    mainWordMultiplier *= ScrabbleGame.this.board.getWordMultiplierAt(newRow, newCol);
                    char placedTile = this.currentlyPlacedTiles[currentTile];

                    if (placedTile != ScrabbleUtil.wildCardTile) {
                        int letterScore = ScrabbleGame.this.letterScoreMap.getScore(placedTile) * ScrabbleGame.this.board.getLetterMultiplierAt(newRow, newCol);
                        mainWordScore += letterScore;
                        perpWordScore += letterScore;
                    }

                    currentTile++;
                } else {
                    if (!ScrabbleGame.this.board.isWildcardAt(newRow, newCol))
                        mainWordScore += ScrabbleGame.this.letterScoreMap.getScore(ScrabbleGame.this.board.getCharAt(newRow, newCol));

                    continue;
                }

                int perpScoreData = this.perpScoreDataSource[newRow][newCol];

                if (perpScoreData == ScrabbleGame.invalidPerpWordScore) continue;

                int perpWordMultiplier = ScrabbleGame.this.board.getWordMultiplierAt(newRow, newCol);
                perpWordScore += perpScoreData;
                playScore += perpWordScore * perpWordMultiplier;
            }

            playScore += mainWordScore * mainWordMultiplier;

            if (this.numPlacedTiles == ScrabbleGame.this.handSize) playScore += 35;

            return playScore;
        }

        private boolean cantPlace(char tile) {
            return !this.validPerpTilesForPlacement[this.numPlacedTiles][ScrabbleUtil.charToInt(tile)];
        }

        private void placeTile(char tile) {
            this.placeTileAs(tile, tile);
        }

        private void placeWildcardTileAs(char as) {
            this.placeTileAs(ScrabbleUtil.wildCardTile, as);
        }

        private void placeTileAs(char tile, char as) {
            this.currentlyPlacedTiles[this.numPlacedTiles] = tile;
            this.effectiveWord[this.posInEffectiveWordMap[this.numPlacedTiles]] = as;
            this.numPlacedTiles++;
        }

        private void removeTile() {
            this.numPlacedTiles--;
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
         a  b  c  d  e  f  g  h  i  j  k  l  m  n  o  p  q  r  s  t  u  v  w  x  y  z
       [ t, f, f, f, t, f, f, f, t, f, f, f, f, f, t, f, f, f, f, f, t, f, f, f, f, f ] <- perpVert[1][3]
       since 'bat', 'bet', 'bit', 'bot', and 'but' are valid words

    --------
    perpHori
    --------

    foo...      a  b  c  d  e  f  g  h  i  j  k  l  m  n  o  p  q  r  s  t  u  v  w  x  y  z
    -----* <- [ f, t, t, f, f, t, f, t, f, f, f, f, t, t, f, t, f, t, t, f, f, f, f, f, f, f ] <- perpHori[0][5]
    .....a    since 'bat', 'cat', 'fat', 'hat', 'mat', 'nat', 'pat', 'rat', and 'sat' are valid words
    .....t

    perpVert and perpHori work to limit what permutations of the playerTiles are used to create words, since
    words will only be valid if they form only valid words in the perpendicular direction

    when it is being considered whether to place a playerTile at a position,
    this.perp(Vert|Hori)[row][col][ScrabbleUtil.charToInt(char)] will show if it is a valid placement perpendicularly
     */
    private final boolean[][][] perpVert;
    private final boolean[][][] perpHori;
    private final PermuteTree permuteTree;
    private final List<WordStart> validWordStarts;
    private final boolean[] possibleCharPlacements;
    private final boolean[][] canPlaceVert;
    private final boolean[][] canPlaceHori;
    private final int handSize;
    private final int[][] perpScoreDataVert; // -1 for invalid
    private final int[][] perpScoreDataHori; // -1 for invalid

    public ScrabbleGame(ILetterScoreMap letterScoreMap, WordGraphDictionary dictionary, IScrabbleBoard board, char[] playerTiles, int handSize) {
        if (dictionary.getRoot() == null) throw new IllegalStateException("Empty dictionary");

        this.letterScoreMap = letterScoreMap;
        this.dictionary = dictionary;
        this.board = board;
        this.playerTiles = playerTiles;
        this.handSize = handSize;
        this.perpVert = new boolean[this.board.getRows()][this.board.getCols()][];
        this.perpHori = new boolean[this.board.getRows()][this.board.getCols()][];
        this.possibleCharPlacements = this.getPossiblePlacements();
        this.canPlaceVert = new boolean[this.board.getRows()][this.board.getCols()];
        this.canPlaceHori = new boolean[this.board.getRows()][this.board.getCols()];
        this.perpScoreDataVert = new int[this.board.getRows()][this.board.getCols()];
        this.perpScoreDataHori = new int[this.board.getRows()][this.board.getCols()];
        this.permuteTree = ScrabbleUtil.timeRetrieval(() -> new PermuteTree(this.playerTiles), "generatePermuteTree");

        ScrabbleUtil.timeIt(this::initializeValidPerpendicularPlacements, "initializeValidPerpendicularPlacements");

        this.validWordStarts = ScrabbleUtil.timeRetrieval(this::findValidWordStarts, "findValidPlacements");
    }

    public List<Move> findMoves() {
        List<Move> moves = new ArrayList<>();

        this.validWordStarts.forEach(wordStart -> this.addAllMovesFromWordStart(wordStart, moves));

        return moves;
    }

    private void addAllMovesFromWordStart(WordStart wordStart, List<Move> moves) {
        WordGraphDictionary.WGNode startPath = this.initializePath(wordStart);

        if (startPath == null) {
            System.out.println("PANIC: placement with invalid prefix | " + Arrays.toString(wordStart.effectiveWord));

            return;
        }

        this.permuteOnWordStart(wordStart, this.permuteTree.getRoot(), startPath, moves);
    }

    private void permuteOnWordStart(WordStart wordStart, PermuteTree.PTNode perm, WordGraphDictionary.WGNode path, List<Move> moves) {
        if (wordStart.numPlacedTiles >= wordStart.minTilesPlaced && path.isWordHere()) {
            char[] playedTilesCopy = new char[wordStart.numPlacedTiles];

            System.arraycopy(wordStart.currentlyPlacedTiles, 0, playedTilesCopy, 0, playedTilesCopy.length);
            moves.add(
                new Move(
                    path.getWord(),
                    playedTilesCopy,
                    wordStart.isVertical,
                    wordStart.row,
                    wordStart.col,
                    wordStart.getScore()
                )
            );
        }

        if (wordStart.numPlacedTiles == wordStart.maxTilesPlaced) {
            return;
        }

        for (Character c : perm.getPaths()) {
            if (c == ScrabbleUtil.wildCardTile) {
                for (char as : ScrabbleUtil.alphaChars) {
                    if (wordStart.cantPlace(as)) continue;

                    WordGraphDictionary.WGNode newPath = this.followPathToNextBlank(wordStart, path, as);

                    if (newPath == null) continue;

                    wordStart.placeWildcardTileAs(as);
                    this.permuteOnWordStart(wordStart, perm.getPath(c), newPath, moves);
                    wordStart.removeTile();
                }
            } else {
                if (wordStart.cantPlace(c)) continue;

                WordGraphDictionary.WGNode newPath = this.followPathToNextBlank(wordStart, path, c);

                if (newPath == null) continue;

                wordStart.placeTile(c);
                this.permuteOnWordStart(wordStart, perm.getPath(c), newPath, moves);
                wordStart.removeTile();
            }
        }
    }

    private WordGraphDictionary.WGNode initializePath(WordStart wordStart) {
        int start = 0;
        int finish = wordStart.posInEffectiveWordMap[0];
        int current = start;
        WordGraphDictionary.WGNode newPath = this.dictionary.getRoot();

        while (current < finish) {
            newPath = newPath.getPath(wordStart.effectiveWord[current]);

            if (newPath == null) break;

            current++;
        }

        return newPath;
    }

    private WordGraphDictionary.WGNode followPathToNextBlank(WordStart wordStart, WordGraphDictionary.WGNode currentPath, char toPlace) {
        int start = wordStart.posInEffectiveWordMap[wordStart.numPlacedTiles];
        int finish = wordStart.effectiveWordSizeMap[wordStart.numPlacedTiles];
        int current = start + 1;
        WordGraphDictionary.WGNode newPath = currentPath.getPath(toPlace);

        if (newPath == null) return null;

        while (current < finish) {
            newPath = newPath.getPath(wordStart.effectiveWord[current]);

            if (newPath == null) break;

            current++;
        }

        return newPath;
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

    private List<WordStart> findValidWordStarts() {
        List<WordStart> wordStarts = new LinkedList<>();

        for (int row = 0; row < this.board.getRows(); row++) {
            for (int col = 0; col < this.board.getCols(); col++) {
                this.addWordStartIfValid(row, col, true, wordStarts);
                this.addWordStartIfValid(row, col, false, wordStarts);
            }
        }

        return wordStarts;
    }

    private void addWordStartIfValid(int row, int col, boolean isVertical, List<WordStart> wordStarts) {
        int wordStart = isVertical ? row : col;
        Offset offset = isVertical ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
        boolean beforeEmpty = wordStart == 0 || this.board.isEmptyAt(offset.newRow(row, -1), offset.newCol(col, -1));

        if (!beforeEmpty) return; // letter before this position

        int wordStartPerp = isVertical ? col : row;
        Offset offsetPerp = isVertical ? ScrabbleGame.horiOffset : ScrabbleGame.vertOffset;
        int lineBound = isVertical ? this.board.getRows() : this.board.getCols();
        int lineBoundPerp = isVertical ? this.board.getCols() : this.board.getRows();
        boolean[][] canPlaceSource = isVertical ? this.canPlaceVert : this.canPlaceHori;
        int blanks = 0;
        int tillBound = lineBound - wordStart;
        boolean hasAnchor = false;
        int blanksTillAnchor = 0;
        int w;

        for (w = 0; w < tillBound; w++) {
            int newRow = offset.newRow(row, w);
            int newCol = offset.newCol(col, w);
            boolean isBlank = this.board.isEmptyAt(newRow, newCol);

            if (isBlank) {
                if (blanks == this.playerTiles.length || !canPlaceSource[newRow][newCol]) break;

                blanks++;
            }

            if (!hasAnchor) {
                boolean onBeforeSide = wordStartPerp > 0 && !this.board.isEmptyAt(offsetPerp.newRow(newRow, -1), offsetPerp.newCol(newCol, -1));
                boolean onAfterSide = wordStartPerp < lineBoundPerp - 1 && !this.board.isEmptyAt(offsetPerp.newRow(newRow, 1), offsetPerp.newCol(newCol, 1));
                boolean onAnchor = newRow == this.board.getAnchorRow() && newCol == this.board.getAnchorCol();
                boolean anchorable = !isBlank || onBeforeSide || onAfterSide || onAnchor;

                if (anchorable) {
                    hasAnchor = true;
                    blanksTillAnchor = blanks;
                }
            }
        }

        if (!hasAnchor || blanks == 0) return;

        int maxWordLength = w;
        char[] effectiveWord = new char[maxWordLength];
        int[] posInEffectiveWordMap = new int[blanks];
        int[] effectiveWordSizeMap = new int[blanks];
        int b = 0;

        for (int i = 0; i < maxWordLength; i++) {
            int newRow = offset.newRow(row, i);
            int newCol = offset.newCol(col, i);
            effectiveWord[i] = this.board.getCharAt(newRow, newCol);
            boolean isBlank = this.board.isEmptyAt(newRow, newCol);

            if (isBlank) {
                posInEffectiveWordMap[b] = i;
                b++;
            }
        }

        System.arraycopy(posInEffectiveWordMap, 1, effectiveWordSizeMap, 0, effectiveWordSizeMap.length - 1);

        effectiveWordSizeMap[effectiveWordSizeMap.length - 1] = maxWordLength;

        wordStarts.add(
             new WordStart(
                 row,
                 col,
                 isVertical,
                 Math.max(1, blanksTillAnchor),
                 blanks,
                 effectiveWord,
                 posInEffectiveWordMap,
                 effectiveWordSizeMap
             )
        );
    }

    private void initializeValidPerpendicularPlacements() {
        for (int row = 0; row < this.board.getRows(); row++) {
            for (int col = 0; col < this.board.getCols(); col++) {
                if (!this.board.isEmptyAt(row, col)) { // tile already placed, nothing valid
                    this.perpVert[row][col] = ScrabbleGame.allInvalid;
                    this.perpHori[row][col] = ScrabbleGame.allInvalid;
                    this.perpScoreDataVert[row][col] = ScrabbleGame.invalidPerpWordScore;
                    this.perpScoreDataHori[row][col] = ScrabbleGame.invalidPerpWordScore;

                    continue;
                }

                this.initializeValidPerpendicularPlacementAt(row, col, true);
                this.initializeValidPerpendicularPlacementAt(row, col, false);
            }
        }
    }

    private void initializeValidPerpendicularPlacementAt(int row, int col, boolean isVertical) {
        boolean[][][] perpSource = isVertical ? this.perpVert : this.perpHori;
        int[][] perpScoreDataSource = isVertical ? this.perpScoreDataVert : this.perpScoreDataHori;
        boolean[][] canPlaceSource = isVertical ? this.canPlaceVert : this.canPlaceHori;
        int wordStartPerp = isVertical ? col : row;
        int lineBoundPerp = isVertical ? this.board.getCols() : this.board.getRows();
        Offset offsetPerp = isVertical ? ScrabbleGame.horiOffset : ScrabbleGame.vertOffset;
        boolean beforeEmpty = wordStartPerp == 0 || this.board.isEmptyAt(offsetPerp.newRow(row, -1), offsetPerp.newCol(col, -1));
        boolean afterEmpty = wordStartPerp == lineBoundPerp - 1 || this.board.isEmptyAt(offsetPerp.newRow(row, 1), offsetPerp.newCol(col, 1));

        if (beforeEmpty && afterEmpty) { // nothing before or after, anything is valid for placement
            perpSource[row][col] = ScrabbleGame.allValid;
            canPlaceSource[row][col] = true;
            perpScoreDataSource[row][col] = ScrabbleGame.invalidPerpWordScore;

            return;
        }

        int score = 0;
        int before = wordStartPerp, after = wordStartPerp;
        int i = 0;

        while (before - 1 >= 0 && !this.board.isEmptyAt(offsetPerp.newRow(row, i - 1), offsetPerp.newCol(col, i - 1))) {
            int newRow = offsetPerp.newRow(row, i - 1), newCol = offsetPerp.newCol(col, i - 1);

            if (!this.board.isWildcardAt(newRow, newCol))
                score += this.letterScoreMap.getScore(this.board.getCharAt(newRow, newCol));

            before--;
            i--;
        }

        WordGraphDictionary.WGNode currentPath = this.dictionary.getRoot();
        int current = before;

        while (current < wordStartPerp) {
            currentPath = currentPath.getPath(this.board.getCharAt(offsetPerp.newRow(row, i), offsetPerp.newCol(col, i)));

            if (currentPath == null) break;

            current++;
            i++;
        }

        if (current == wordStartPerp) { // before tile is valid prefix
            boolean[] result = new boolean[ScrabbleUtil.alphaChars.length];
            int scoreData;
            i = 0;

            while (after + 1 < lineBoundPerp && !this.board.isEmptyAt(offsetPerp.newRow(row, i + 1), offsetPerp.newCol(col, i + 1))) {
                int newRow = offsetPerp.newRow(row, i + 1), newCol = offsetPerp.newCol(col, i + 1);

                if (!this.board.isWildcardAt(newRow, newCol))
                    score += this.letterScoreMap.getScore(this.board.getCharAt(newRow, newCol));

                after++;
                i++;
            }

            boolean hasAny = false;

            for (Character c : currentPath.getPaths()) {
                if (!this.possibleCharPlacements[ScrabbleUtil.charToInt(c)]) continue;

                WordGraphDictionary.WGNode afterPath = currentPath.getPath(c);
                current = wordStartPerp + 1;
                i = 1;

                while (current <= after) {
                    afterPath = afterPath.getPath(this.board.getCharAt(offsetPerp.newRow(row, i), offsetPerp.newCol(col, i)));

                    if (afterPath == null) break;

                    current++;
                    i++;
                }

                if (afterPath != null && afterPath.isWordHere()) {
                    result[ScrabbleUtil.charToInt(c)] = true;
                    hasAny = true;
                }
            }

            if (hasAny) {
                canPlaceSource[row][col] = true;
                scoreData = score;
            } else {
                scoreData = ScrabbleGame.invalidPerpWordScore;
            }

            perpSource[row][col] = result;
            perpScoreDataSource[row][col] = scoreData;
        } else {
            perpSource[row][col] = ScrabbleGame.allInvalid;
            perpScoreDataSource[row][col] = ScrabbleGame.invalidPerpWordScore;
        }
    }

}
