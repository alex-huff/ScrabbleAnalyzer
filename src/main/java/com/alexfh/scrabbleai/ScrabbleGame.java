package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.ai.PermuteTree;
import com.alexfh.scrabbleai.dictionary.WordGraphDictionary;
import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.rule.ILetterScoreMap;
import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.util.*;

public class ScrabbleGame {

    private static final boolean[] allValid = new boolean[ScrabbleUtil.alphaChars.length];
    private static final boolean[] allInvalid = new boolean[ScrabbleUtil.alphaChars.length];
    private static final PerpScoreData invalidPerpWordScore = new PerpScoreData(false, 0);
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

    private record PerpScoreData(boolean hasPerpWord, int score) { }

    private class Placement {

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
        private final PerpScoreData[][] perpScoreDataSource;
        private final Offset offset;

        private Placement(int row, int col, boolean isVertical, int minTilesPlaced, int maxTilesPlaced, char[] effectiveWord, int[] posInEffectiveWordMap, int[] effectiveWordSizeMap) {
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

                PerpScoreData perpScoreData = this.perpScoreDataSource[newRow][newCol];

                if (!perpScoreData.hasPerpWord) continue;

                int perpWordMultiplier = ScrabbleGame.this.board.getWordMultiplierAt(newRow, newCol);
                perpWordScore += perpScoreData.score();
                playScore += perpWordScore * perpWordMultiplier;
            }

            playScore += mainWordScore * mainWordMultiplier;

            if (this.numPlacedTiles == ScrabbleGame.this.handSize) playScore += 35;

            return playScore;
        }

        private boolean canPlace(char tile) {
            return this.validPerpTilesForPlacement[this.numPlacedTiles][ScrabbleUtil.charToInt(tile)];
        }

        private boolean placeTile(char tile) {
            return this.placeTileAs(tile, tile);
        }

        private boolean placeWildcardTileAs(char as) {
            return this.placeTileAs(ScrabbleUtil.wildCardTile, as);
        }

        private boolean placeTileAs(char tile, char as) {
            if (!this.canPlace(as)) {
                return false;
            }

            this.currentlyPlacedTiles[this.numPlacedTiles] = tile;
            this.effectiveWord[this.posInEffectiveWordMap[this.numPlacedTiles]] = as;
            this.numPlacedTiles++;

            return true;
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
    private final int handSize;
    private final PerpScoreData[][] perpScoreDataVert;
    private final PerpScoreData[][] perpScoreDataHori;

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
        this.perpScoreDataVert = new PerpScoreData[this.board.getRows()][this.board.getCols()];
        this.perpScoreDataHori = new PerpScoreData[this.board.getRows()][this.board.getCols()];
        this.permuteTree = ScrabbleUtil.timeRetrieval(() -> new PermuteTree(this.playerTiles), "generatePermuteTree");

        ScrabbleUtil.timeIt(this::initializeValidPerpendicularPlacements, "initializeValidPerpendicularPlacements");

        this.validPlacements = ScrabbleUtil.timeRetrieval(this::findValidPlacements, "findValidPlacements");
    }

    public List<Move> findMoves() {
        List<Move> moves = new ArrayList<>();

        this.validPlacements.stream().sequential().forEach(
            placement -> moves.addAll(this.getMovesFromPlacement(placement))
        );

        return moves;
    }

    private List<Move> getMovesFromPlacement(Placement placement) {
        List<Move> moves = new LinkedList<>();
        WordGraphDictionary.WGNode startPath = this.initializePath(placement);

        if (startPath == null) {
            System.out.println("PANIC: placement with invalid prefix");

            return moves;
        }

        this.permuteOnPlacement(placement, this.permuteTree.getRoot(), startPath, moves);

        return moves;
    }

    private void permuteOnPlacement(Placement placement, PermuteTree.PTNode perm, WordGraphDictionary.WGNode path, List<Move> moves) {
        if (placement.numPlacedTiles >= placement.minTilesPlaced && path.isWordHere()) {
            char[] playedTilesCopy = new char[placement.numPlacedTiles];

            System.arraycopy(placement.currentlyPlacedTiles, 0, playedTilesCopy, 0, playedTilesCopy.length);
            moves.add(
                new Move(
                    path.getWord(),
                    playedTilesCopy,
                    placement.isVertical,
                    placement.row,
                    placement.col,
                    placement.getScore()
                )
            );
        }

        if (placement.numPlacedTiles == placement.maxTilesPlaced) {
            return;
        }

        for (Character c : perm.getPaths()) {
            if (c == ScrabbleUtil.wildCardTile) {
                for (char as : ScrabbleUtil.alphaChars) {
                    if (!placement.canPlace(as)) continue;

                    WordGraphDictionary.WGNode newPath = this.followPlacementPath(placement, path, as);

                    if (newPath != null) {
                        placement.placeWildcardTileAs(as);
                        this.permuteOnPlacement(placement, perm.getPath(c), newPath, moves);
                        placement.removeTile();
                    }
                }
            } else {
                if (!placement.canPlace(c)) continue;

                WordGraphDictionary.WGNode newPath = this.followPlacementPath(placement, path, c);

                if (newPath != null) {
                    placement.placeTile(c);
                    this.permuteOnPlacement(placement, perm.getPath(c), newPath, moves);
                    placement.removeTile();
                }
            }
        }
    }

    private WordGraphDictionary.WGNode initializePath(Placement placement) {
        int start = 0;
        int finish = placement.posInEffectiveWordMap[0];
        int current = start;
        WordGraphDictionary.WGNode newPath = this.dictionary.getRoot();

        while (current < finish) {
            newPath = newPath.getPath(placement.effectiveWord[current]);

            if (newPath == null) break;

            current++;
        }

        return newPath;
    }

    private WordGraphDictionary.WGNode followPlacementPath(Placement placement, WordGraphDictionary.WGNode currentPath, char toPlace) {
        int start = placement.posInEffectiveWordMap[placement.numPlacedTiles];
        int finish = placement.effectiveWordSizeMap[placement.numPlacedTiles];
        int current = start + 1;
        WordGraphDictionary.WGNode newPath = currentPath.getPath(toPlace);

        if (newPath == null) return null;

        while (current < finish) {
            newPath = newPath.getPath(placement.effectiveWord[current]);

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

    private List<Placement> findValidPlacements() {
        List<Placement> placements = new LinkedList<>();

        for (int row = 0; row < this.board.getRows(); row++) {
            for (int col = 0; col < this.board.getCols(); col++) {
                this.addPlacementIfValid(row, col, true, placements);
                this.addPlacementIfValid(row, col, false, placements);
            }
        }

        return placements;
    }

    private void addPlacementIfValid(int row, int col, boolean isVertical, List<Placement> placements) {
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

        placements.add(
             new Placement(
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

                this.initializeValidReplacements(row, col, true);
                this.initializeValidReplacements(row, col, false);
            }
        }
    }

    private void initializeValidReplacements(int row, int col, boolean isVertical) {
        boolean[][][] perpSource = isVertical ? this.perpVert : this.perpHori;
        PerpScoreData[][] perpScoreDataSource = isVertical ? this.perpScoreDataVert : this.perpScoreDataHori;
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
            PerpScoreData scoreData;
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
                scoreData = new PerpScoreData(true, score);
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
