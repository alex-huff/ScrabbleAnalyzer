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
    private static final PerpScoreData invalidPerpWord = new PerpScoreData(false, 0);

    static {
        Arrays.fill(ScrabbleGame.allValid, true);
    }

    public record Move(String playedWord, char[] playedTiles, boolean isVertical, int row, int col, int score) implements Comparable<Move> {

        private static final Comparator<Move> moveComparator = Comparator.comparingInt(
            Move::score
        ).reversed().thenComparingInt(
            move -> move.playedWord.length()
        ).thenComparing(
            Move::playedWord
        );

        @Override
        public int compareTo(Move o) {
            return moveComparator.compare(this, o);
        }

    }

    public record PerpScoreData(boolean hasPerpWord, int score) { }

    public class Placement {

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

        public Placement(int row, int col, boolean isVertical, int minTilesPlaced, int maxTilesPlaced, char[] effectiveWord, int[] posInEffectiveWordMap, int[] effectiveWordSizeMap) {
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

            for (int i = 0; i < this.validPerpTilesForPlacement.length; i++) {
                int spotInWord = this.posInEffectiveWordMap[i];
                this.validPerpTilesForPlacement[i] = this.isVertical ?
                    ScrabbleGame.this.perpVert[row + spotInWord][col] :
                    ScrabbleGame.this.perpHori[row][col + spotInWord];
            }
        }

        public int getScore() {
            return this.isVertical ? this.getScoreVert() : this.getScoreHori();
        }

        private int getScoreVert() {
            int playScore = 0;
            int mainWordScore = 0;
            int mainWordMultiplier = 1;
            int currentTile = 0;

            for (int i = 0; i < this.effectiveWordSizeMap[this.numPlacedTiles - 1]; i++) {
                int perpWordScore = 0;

                if (ScrabbleGame.this.board.isEmptyAt(row + i, col)) {
                    mainWordMultiplier *= ScrabbleGame.this.board.getWordMultiplierAt(row + i, col);
                    char placedTile = this.currentlyPlacedTiles[currentTile];

                    if (placedTile != ScrabbleUtil.wildCardTile) {
                        int letterScore = ScrabbleGame.this.letterScoreMap.getScore(placedTile) * ScrabbleGame.this.board.getLetterMultiplierAt(row + i, col);
                        mainWordScore += letterScore;
                        perpWordScore += letterScore;
                    }

                    currentTile++;
                } else {
                    if (!ScrabbleGame.this.board.isWildcardAt(row + i, col))
                        mainWordScore += ScrabbleGame.this.letterScoreMap.getScore(ScrabbleGame.this.board.getCharAt(row + i, col));

                    continue;
                }

                PerpScoreData perpScoreData = ScrabbleGame.this.perpScoreDataVert[row + i][col];

                if (!perpScoreData.hasPerpWord) continue;

                int perpWordMultiplier = ScrabbleGame.this.board.getWordMultiplierAt(row + i, col);
                perpWordScore += perpScoreData.score();
                playScore += perpWordScore * perpWordMultiplier;
            }

            playScore += mainWordScore * mainWordMultiplier;

            if (this.numPlacedTiles == ScrabbleGame.this.handSize) playScore += 35;

            return playScore;
        }

        private int getScoreHori() {
            int playScore = 0;
            int mainWordScore = 0;
            int mainWordMultiplier = 1;
            int currentTile = 0;

            for (int i = 0; i < this.effectiveWordSizeMap[this.numPlacedTiles - 1]; i++) {
                int perpWordScore = 0;

                if (ScrabbleGame.this.board.isEmptyAt(row, col + i)) {
                    mainWordMultiplier *= ScrabbleGame.this.board.getWordMultiplierAt(row, col + i);
                    char placedTile = this.currentlyPlacedTiles[currentTile];

                    if (placedTile != ScrabbleUtil.wildCardTile) {
                        int letterScore = ScrabbleGame.this.letterScoreMap.getScore(placedTile) * ScrabbleGame.this.board.getLetterMultiplierAt(row, col + i);
                        mainWordScore += letterScore;
                        perpWordScore += letterScore;
                    }

                    currentTile++;
                } else {
                    if (!ScrabbleGame.this.board.isWildcardAt(row, col + i))
                        mainWordScore += ScrabbleGame.this.letterScoreMap.getScore(ScrabbleGame.this.board.getCharAt(row, col + i));

                    continue;
                }

                PerpScoreData perpScoreData = ScrabbleGame.this.perpScoreDataHori[row][col + i];

                if (!perpScoreData.hasPerpWord) continue;

                int perpWordMultiplier = ScrabbleGame.this.board.getWordMultiplierAt(row, col + i);
                perpWordScore += perpScoreData.score();
                playScore += perpWordScore * perpWordMultiplier;
            }

            playScore += mainWordScore * mainWordMultiplier;

            if (this.numPlacedTiles == ScrabbleGame.this.handSize) playScore += 35;

            return playScore;
        }

        public boolean canPlace(char tile) {
            return this.validPerpTilesForPlacement[this.numPlacedTiles][ScrabbleUtil.charToInt(tile)];
        }

        public boolean placeTile(char tile) {
            return this.placeTileAs(tile, tile);
        }

        public boolean placeWildcardTileAs(char as) {
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

        public void removeTile() {
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
        if (placement.numPlacedTiles >= placement.minTilesPlaced && path.wordHere) {
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
            Math.max(1, blanksTillAnchor),
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
            Math.max(1, blanksTillAnchor),
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
                    this.perpScoreDataVert[row][col] = ScrabbleGame.invalidPerpWord;
                    this.perpScoreDataHori[row][col] = ScrabbleGame.invalidPerpWord;

                    continue;
                }

                boolean leftEmpty = col == 0 || this.board.isEmptyAt(row, col - 1);
                boolean rightEmpty = col == this.board.getCols() - 1 || this.board.isEmptyAt(row, col + 1);
                boolean topEmpty = row == 0 || this.board.isEmptyAt(row - 1, col);
                boolean bottomEmpty = row == this.board.getRows() - 1 || this.board.isEmptyAt(row + 1, col);

                if (leftEmpty && rightEmpty) { // nothing to left or right, anything is valid for vertical placement
                    this.perpVert[row][col] = ScrabbleGame.allValid;
                    this.canPlaceVert[row][col] = true;
                    this.perpScoreDataVert[row][col] = ScrabbleGame.invalidPerpWord;
                } else {
                    this.generateValidReplacementsVert(row, col);
                }

                if (topEmpty && bottomEmpty) { // nothing above or below, anything is valid for horizontal placement
                    this.perpHori[row][col] = ScrabbleGame.allValid;
                    this.canPlaceHori[row][col] = true;
                    this.perpScoreDataHori[row][col] = ScrabbleGame.invalidPerpWord;
                } else {
                    this.generateValidReplacementsHori(row, col);
                }
            }
        }
    }

    private void generateValidReplacementsVert(int row, int col) {
        int score = 0;
        int left = col, right = col;

        while (left - 1 >= 0 && !this.board.isEmptyAt(row, left - 1)) {
            if (!this.board.isWildcardAt(row, left - 1))
                score += this.letterScoreMap.getScore(this.board.getCharAt(row, left - 1));

            left--;
        }

        WordGraphDictionary.WGNode currentPath = this.dictionary.getRoot();
        int current = left;

        while (current < col) {
            currentPath = currentPath.getPath(this.board.getCharAt(row, current));

            if (currentPath == null) break;

            current++;
        }

        if (current == col) { // left of tile is valid prefix
            boolean[] result = new boolean[ScrabbleUtil.alphaChars.length];
            PerpScoreData scoreData;

            while (right + 1 < this.board.getCols() && !this.board.isEmptyAt(row, right + 1)) {
                if (!this.board.isWildcardAt(row, right + 1))
                    score += this.letterScoreMap.getScore(this.board.getCharAt(row, right + 1));

                right++;
            }

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

            if (hasAny) {
                this.canPlaceVert[row][col] = true;
                scoreData = new PerpScoreData(true, score);
            } else {
                scoreData = ScrabbleGame.invalidPerpWord;
            }

            this.perpVert[row][col] = result;
            this.perpScoreDataVert[row][col] = scoreData;
        } else {
            this.perpVert[row][col] = ScrabbleGame.allInvalid;
            this.perpScoreDataVert[row][col] = ScrabbleGame.invalidPerpWord;
        }
    }

    private void generateValidReplacementsHori(int row, int col) {
        int score = 0;
        int top = row, bottom = row;

        while (top - 1 >= 0 && !this.board.isEmptyAt(top - 1, col)) {
            if (!this.board.isWildcardAt(top - 1, col))
                score += this.letterScoreMap.getScore(this.board.getCharAt(top - 1, col));

            top--;
        }

        WordGraphDictionary.WGNode currentPath = this.dictionary.getRoot();
        int current = top;

        while (current < row) {
            currentPath = currentPath.getPath(this.board.getCharAt(current, col));

            if (currentPath == null) break;

            current++;
        }

        if (current == row) { // top of tile is valid prefix
            boolean[] result = new boolean[ScrabbleUtil.alphaChars.length];
            PerpScoreData scoreData;

            while (bottom + 1 < this.board.getRows() && !this.board.isEmptyAt(bottom + 1, col)) {
                if (!this.board.isWildcardAt(bottom + 1, col))
                    score += this.letterScoreMap.getScore(this.board.getCharAt(bottom + 1, col));

                bottom++;
            }

            boolean hasAny = false;

            for (Character c : currentPath.getPaths()) {
                if (!this.possibleCharPlacements[ScrabbleUtil.charToInt(c)]) continue;

                WordGraphDictionary.WGNode rightPath = currentPath.getPath(c);
                current = row + 1;

                while (current <= bottom) {
                    rightPath = rightPath.getPath(this.board.getCharAt(current, col));

                    if (rightPath == null) break;

                    current++;
                }

                if (rightPath != null && rightPath.wordHere) {
                    result[ScrabbleUtil.charToInt(c)] = true;
                    hasAny = true;
                }
            }

            if (hasAny) {
                this.canPlaceHori[row][col] = true;
                scoreData = new PerpScoreData(true, score);
            } else {
                scoreData = ScrabbleGame.invalidPerpWord;
            }

            this.perpHori[row][col] = result;
            this.perpScoreDataHori[row][col] = scoreData;
        } else {
            this.perpHori[row][col] = ScrabbleGame.allInvalid;
            this.perpScoreDataHori[row][col] = ScrabbleGame.invalidPerpWord;
        }
    }

}
