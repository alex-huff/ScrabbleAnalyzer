package com.alexfh.scrabbleanalyzer;

import com.alexfh.scrabbleanalyzer.ai.PermuteTree;
import com.alexfh.scrabbleanalyzer.dictionary.WordGraphDictionary;
import com.alexfh.scrabbleanalyzer.rule.ILetterScoreMap;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;
import com.alexfh.scrabbleanalyzer.util.ScrabbleUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScrabbleGame
{

    public static final  ExecutorService threadPool           = Executors.newCachedThreadPool();
    private static final boolean[]       allValid             = new boolean[IScrabbleGameState.alphaChars.length];
    private static final boolean[]       allInvalid           = new boolean[IScrabbleGameState.alphaChars.length];
    private static final int             invalidPerpWordScore = -1;
    public static final  Offset          vertOffset           = new Offset(1, 0);
    public static final  Offset          horiOffset           = new Offset(0, 1);

    static
    {
        Arrays.fill(ScrabbleGame.allValid, true);
    }

    public static record Offset(int moveRowBy, int moveColBy)
    {

        public int newRow(int row, int offset)
        {
            return row + this.moveRowBy * offset;
        }

        public int newCol(int col, int offset)
        {
            return col + this.moveColBy * offset;
        }

    }

    public record Move(
        String playedWord, char[] playedTiles, int[] tileSpotsInWord, boolean isVertical, int row, int col, int score
    ) implements Comparable<Move>
    {

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
        public int compareTo(Move o)
        {
            return moveComparator.compare(this, o);
        }

    }

    private class WordStart
    {

        private final int         row;
        private final int         col;
        private final boolean     isVertical;
        private final int         minTilesPlaced;
        private final int         maxTilesPlaced;
        private final char[]      currentlyPlacedTiles;
        private final char[]      effectiveWord;
        private final int[]       posInEffectiveWordMap;
        private final int[]       effectiveWordSizeMap;
        private       int         numPlacedTiles;
        private final boolean[][] validPerpTilesForPlacement;
        private final int[][][]   scoreDataSource;
        private final int[][]     perpScoreDataSource;
        private final int[][]     cumulativeScoringData;
        private final Offset      offset;

        private WordStart(
            int row, int col, boolean isVertical, int minTilesPlaced, int maxTilesPlaced, char[] effectiveWord,
            int[] posInEffectiveWordMap, int[] effectiveWordSizeMap
        )
        {
            this.row                        = row;
            this.col                        = col;
            this.isVertical                 = isVertical;
            this.minTilesPlaced             = minTilesPlaced;
            this.maxTilesPlaced             = maxTilesPlaced;
            this.currentlyPlacedTiles       = new char[this.maxTilesPlaced];
            this.effectiveWord              = effectiveWord;
            this.posInEffectiveWordMap      = posInEffectiveWordMap;
            this.effectiveWordSizeMap       = effectiveWordSizeMap;
            this.validPerpTilesForPlacement = new boolean[this.maxTilesPlaced][];
            this.scoreDataSource            = this.isVertical ? ScrabbleGame.this.scoreDataVert
                                                              : ScrabbleGame.this.scoreDataHori;
            this.perpScoreDataSource        = this.isVertical ? ScrabbleGame.this.perpScoreDataVert
                                                              : ScrabbleGame.this.perpScoreDataHori;
            this.cumulativeScoringData
                                            = new int[this.maxTilesPlaced][]; // letterMultiplier, perpWordMultiplier, cumulativePlayScore, cumulativeMainWordScore, cumulativeMainWordMultiplier
            this.offset                     = this.isVertical ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
            boolean[][][] validPerpTilesSource = this.isVertical ? ScrabbleGame.this.perpVert
                                                                 : ScrabbleGame.this.perpHori;

            for (int i = 0; i < validPerpTilesForPlacement.length; i++)
            {
                int spotInWord = this.posInEffectiveWordMap[i];
                int newRow     = offset.newRow(this.row, spotInWord);
                int newCol     = offset.newCol(this.col, spotInWord);
                this.validPerpTilesForPlacement[i] = validPerpTilesSource[newRow][newCol];
            }
        }

        // lazily cache scoring data for words based on number of tiles
        private int[] getScoreDataForTile(int tile)
        {
            if (this.cumulativeScoringData[tile] != null) return this.cumulativeScoringData[tile];

            int   spotInWord       = this.posInEffectiveWordMap[tile];
            int   newRow           = offset.newRow(this.row, spotInWord);
            int   newCol           = offset.newCol(this.col, spotInWord);
            int[] normalScoreData  = this.scoreDataSource[newRow][newCol];
            int   perpScoreData    = this.perpScoreDataSource[newRow][newCol];
            int   letterMultiplier = normalScoreData[0];
            int   wordMultiplier   = ScrabbleGame.this.gameState.getWordMultiplierAt(newRow, newCol);
            int   prefixScore      = normalScoreData[2];
            int   suffixScore      = normalScoreData[3];
            int[] current          = new int[5];
            current[0] = letterMultiplier;
            current[1] = (perpScoreData == ScrabbleGame.invalidPerpWordScore) ? 0 : wordMultiplier;

            if (tile == 0)
            {
                current[2] = current[1] * perpScoreData;
                current[3] = prefixScore + suffixScore;
                current[4] = wordMultiplier;
            }
            else
            {
                int[] last = this.getScoreDataForTile(tile - 1);
                current[2] = last[2] + current[1] * perpScoreData;
                current[3] = last[3] + suffixScore;
                current[4] = last[4] * wordMultiplier;

                if (tile + 1 == ScrabbleGame.this.gameState.getRackSize()) current[2] += 35;
            }

            this.cumulativeScoringData[tile] = current;

            return this.cumulativeScoringData[tile];
        }

        private int getScore()
        {
            int[] cumulativeData     = this.getScoreDataForTile(this.numPlacedTiles - 1);
            int   playScore          = cumulativeData[2];
            int   mainWordScore      = cumulativeData[3];
            int   mainWordMultiplier = cumulativeData[4];

            for (int i = 0; i < this.numPlacedTiles; i++)
            {
                char  placedTile         = this.currentlyPlacedTiles[i];
                int[] tileScoringData
                                         = this.cumulativeScoringData[i]; // won't be null because this.getScoreDataForTile(this.numPlacedTiles - 1); will recursively initialize all the way to 0
                int   letterMultiplier   = tileScoringData[0];
                int   perpWordMultiplier = tileScoringData[1];
                int   letterScore        = (placedTile == IScrabbleGameState.wildCardTile) ? 0 : letterMultiplier *
                                                                                                 ScrabbleGame.this.letterScoreMap.getScore(
                                                                                                     placedTile);
                playScore += letterScore * perpWordMultiplier;
                mainWordScore += letterScore;
            }

            playScore += mainWordMultiplier * mainWordScore;

            return playScore;
        }

        private boolean cantPlace(char tile)
        {
            return !this.validPerpTilesForPlacement[this.numPlacedTiles][ScrabbleUtil.charToInt(tile)];
        }

        private void placeTile(char tile)
        {
            this.placeTileAs(tile, tile);
        }

        private void placeWildcardTileAs(char as)
        {
            this.placeTileAs(IScrabbleGameState.wildCardTile, as);
        }

        private void placeTileAs(char tile, char as)
        {
            this.currentlyPlacedTiles[this.numPlacedTiles]                      = tile;
            this.effectiveWord[this.posInEffectiveWordMap[this.numPlacedTiles]] = as;
            this.numPlacedTiles++;
        }

        private void removeTile()
        {
            this.numPlacedTiles--;
        }

    }

    private final ILetterScoreMap     letterScoreMap;
    private final WordGraphDictionary dictionary;
    private final IScrabbleGameState  gameState;

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
    private final boolean[][][]   perpVert;
    private final boolean[][][]   perpHori;
    private       PermuteTree     permuteTree;
    private       List<WordStart> validWordStarts;
    private final boolean[]       possibleCharPlacements;
    private final boolean[][]     canPlaceVert;
    private final boolean[][]     canPlaceHori;
    private final int[][][]       scoreDataVert;
        // letterMultiplier, wordMultiplier, prefixPlacedTileScores, suffixPlacedTileScores | null for invalid
    private final int[][][]       scoreDataHori;
        // letterMultiplier, wordMultiplier, prefixPlacedTileScores, suffixPlacedTileScores | null for invalid
    private final int[][]         perpScoreDataVert; // -1 for invalid
    private final int[][]         perpScoreDataHori; // -1 for invalid
    private       boolean         initialized = false;

    public ScrabbleGame(ILetterScoreMap letterScoreMap, WordGraphDictionary dictionary, IScrabbleGameState gameState)
    {
        if (dictionary.getRoot() == null) throw new IllegalStateException("Empty dictionary");

        this.letterScoreMap         = letterScoreMap;
        this.dictionary             = dictionary;
        this.gameState              = gameState;
        this.perpVert               = new boolean[this.gameState.getRows()][this.gameState.getCols()][];
        this.perpHori               = new boolean[this.gameState.getRows()][this.gameState.getCols()][];
        this.possibleCharPlacements = this.getPossiblePlacements();
        this.canPlaceVert           = new boolean[this.gameState.getRows()][this.gameState.getCols()];
        this.canPlaceHori           = new boolean[this.gameState.getRows()][this.gameState.getCols()];
        this.scoreDataVert          = new int[this.gameState.getRows()][this.gameState.getCols()][];
        this.scoreDataHori          = new int[this.gameState.getRows()][this.gameState.getCols()][];
        this.perpScoreDataVert      = new int[this.gameState.getRows()][this.gameState.getCols()];
        this.perpScoreDataHori      = new int[this.gameState.getRows()][this.gameState.getCols()];
    }

    private void initialize() throws InterruptedException
    {
        this.permuteTree = ScrabbleUtil.timeRetrievalInterruptable(
            () -> new PermuteTree(this.gameState.getTilesInRack()), "generatePermuteTree");

        ScrabbleUtil.timeItInterruptable(
            this::initializeValidPerpendicularPlacementsAndScoringData, "initializeValidPerpendicularPlacements");

        this.validWordStarts = ScrabbleUtil.timeRetrievalInterruptable(
            this::findValidWordStarts, "findValidPlacements");
        this.initialized     = true;
    }

    public List<Move> findMoves() throws InterruptedException
    {
        if (!this.initialized) this.initialize();

        List<Move> moves = new ArrayList<>();

        for (WordStart wordStart : this.validWordStarts)
        {
            ScrabbleUtil.checkInterrupted();
            this.addAllMovesFromWordStart(wordStart, moves);
        }

        return moves;
    }

    private void addAllMovesFromWordStart(WordStart wordStart, List<Move> moves)
    {
        WordGraphDictionary.WGNode startPath = this.initializePath(wordStart);

        if (startPath == null)
        {
            System.out.println("PANIC: placement with invalid prefix | " + Arrays.toString(wordStart.effectiveWord));

            return;
        }

        this.permuteOnWordStart(wordStart, this.permuteTree.getRoot(), startPath, moves);
    }

    private void permuteOnWordStart(
        WordStart wordStart, PermuteTree.PTNode perm, WordGraphDictionary.WGNode path, List<Move> moves
    )
    {
        if (wordStart.numPlacedTiles >= wordStart.minTilesPlaced && path.isWordHere())
        {
            char[] playedTilesCopy = new char[wordStart.numPlacedTiles];
            int[]  tileSpotsInWord = new int[wordStart.numPlacedTiles];

            System.arraycopy(wordStart.currentlyPlacedTiles, 0, playedTilesCopy, 0, playedTilesCopy.length);
            System.arraycopy(wordStart.posInEffectiveWordMap, 0, tileSpotsInWord, 0, tileSpotsInWord.length);
            moves.add(
                new Move(
                    path.getWord(),
                    playedTilesCopy,
                    tileSpotsInWord,
                    wordStart.isVertical,
                    wordStart.row,
                    wordStart.col,
                    wordStart.getScore()
                )
            );
        }

        if (wordStart.numPlacedTiles == wordStart.maxTilesPlaced) return;

        WordGraphDictionary.WGNode newPath;

        for (Character c : perm.getPaths())
        {
            if (c == IScrabbleGameState.wildCardTile)
            {
                for (char as : IScrabbleGameState.alphaChars)
                {
                    if (wordStart.cantPlace(as) || (newPath = this.followPathToNextBlank(wordStart, path, as)) == null)
                        continue;

                    wordStart.placeWildcardTileAs(as);
                    this.permuteOnWordStart(wordStart, perm.getPath(c), newPath, moves);
                    wordStart.removeTile();
                }
            }
            else
            {
                if (wordStart.cantPlace(c) || (newPath = this.followPathToNextBlank(wordStart, path, c)) == null)
                    continue;

                wordStart.placeTile(c);
                this.permuteOnWordStart(wordStart, perm.getPath(c), newPath, moves);
                wordStart.removeTile();
            }
        }
    }

    private WordGraphDictionary.WGNode initializePath(WordStart wordStart)
    {
        int                        start   = 0;
        int                        finish  = wordStart.posInEffectiveWordMap[0];
        int                        current = start;
        WordGraphDictionary.WGNode newPath = this.dictionary.getRoot();

        while (current < finish)
        {
            newPath = newPath.getPath(wordStart.effectiveWord[current]);

            if (newPath == null) break;

            current++;
        }

        return newPath;
    }

    private WordGraphDictionary.WGNode followPathToNextBlank(
        WordStart wordStart, WordGraphDictionary.WGNode currentPath, char toPlace
    )
    {
        int                        start   = wordStart.posInEffectiveWordMap[wordStart.numPlacedTiles];
        int                        finish  = wordStart.effectiveWordSizeMap[wordStart.numPlacedTiles];
        int                        current = start + 1;
        WordGraphDictionary.WGNode newPath = currentPath.getPath(toPlace);

        if (newPath == null) return null;

        while (current < finish)
        {
            newPath = newPath.getPath(wordStart.effectiveWord[current]);

            if (newPath == null) break;

            current++;
        }

        return newPath;
    }

    private boolean[] getPossiblePlacements()
    {
        boolean[] placements = new boolean[IScrabbleGameState.alphaChars.length];

        for (Character c : this.gameState.getTilesInRack())
        {
            if (c == IScrabbleGameState.wildCardTile)
            {
                return ScrabbleGame.allValid;
            }

            placements[ScrabbleUtil.charToInt(c)] = true;
        }

        return placements;
    }

    private List<WordStart> findValidWordStarts() throws InterruptedException
    {
        List<WordStart> wordStarts = new LinkedList<>();

        for (int row = 0; row < this.gameState.getRows(); row++)
        {
            for (int col = 0; col < this.gameState.getCols(); col++)
            {
                ScrabbleUtil.checkInterrupted();
                this.addWordStartIfValid(row, col, true, wordStarts);
                this.addWordStartIfValid(row, col, false, wordStarts);
            }
        }

        return wordStarts;
    }

    private void addWordStartIfValid(int row, int col, boolean isVertical, List<WordStart> wordStarts)
    {
        int     wordStart   = isVertical ? row : col;
        Offset  offset      = isVertical ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
        boolean beforeEmpty = wordStart == 0 ||
                              this.gameState.isEmptyAt(offset.newRow(row, -1), offset.newCol(col, -1));

        if (!beforeEmpty) return; // letter before this position

        int         wordStartPerp    = isVertical ? col : row;
        Offset      offsetPerp       = isVertical ? ScrabbleGame.horiOffset : ScrabbleGame.vertOffset;
        int         lineBound        = isVertical ? this.gameState.getRows() : this.gameState.getCols();
        int         lineBoundPerp    = isVertical ? this.gameState.getCols() : this.gameState.getRows();
        boolean[][] canPlaceSource   = isVertical ? this.canPlaceVert : this.canPlaceHori;
        int         blanks           = 0;
        int         tillBound        = lineBound - wordStart;
        boolean     hasAnchor        = false;
        int         blanksTillAnchor = 0;
        int         w;

        for (w = 0; w < tillBound; w++)
        {
            int     newRow  = offset.newRow(row, w);
            int     newCol  = offset.newCol(col, w);
            boolean isBlank = this.gameState.isEmptyAt(newRow, newCol);

            if (isBlank)
            {
                if (blanks == this.gameState.getNumTilesInRack() || !canPlaceSource[newRow][newCol]) break;

                blanks++;
            }

            if (!hasAnchor)
            {
                boolean onBeforeSide = wordStartPerp > 0 && !this.gameState.isEmptyAt(offsetPerp.newRow(newRow, -1),
                                                                                      offsetPerp.newCol(newCol, -1)
                );
                boolean onAfterSide  = wordStartPerp < lineBoundPerp - 1 &&
                                       !this.gameState.isEmptyAt(offsetPerp.newRow(newRow, 1),
                                                                 offsetPerp.newCol(newCol, 1)
                                       );
                boolean onAnchor     = newRow == this.gameState.getAnchorRow() &&
                                       newCol == this.gameState.getAnchorCol();
                boolean anchorable   = !isBlank || onBeforeSide || onAfterSide || onAnchor;

                if (anchorable)
                {
                    hasAnchor        = true;
                    blanksTillAnchor = blanks;
                }
            }
        }

        if (!hasAnchor || blanks == 0) return;

        int    maxWordLength         = w;
        char[] effectiveWord         = new char[maxWordLength];
        int[]  posInEffectiveWordMap = new int[blanks];
        int[]  effectiveWordSizeMap  = new int[blanks];
        int    b                     = 0;

        for (int i = 0; i < maxWordLength; i++)
        {
            int newRow = offset.newRow(row, i);
            int newCol = offset.newCol(col, i);
            effectiveWord[i] = this.gameState.getCharAt(newRow, newCol);
            boolean isBlank = this.gameState.isEmptyAt(newRow, newCol);

            if (isBlank)
            {
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

    private void initializeValidPerpendicularPlacementsAndScoringData() throws InterruptedException
    {
        for (int row = 0; row < this.gameState.getRows(); row++)
        {
            for (int col = 0; col < this.gameState.getCols(); col++)
            {
                ScrabbleUtil.checkInterrupted();

                if (!this.gameState.isEmptyAt(row, col))
                { // tile already placed, nothing valid
                    this.perpVert[row][col]          = ScrabbleGame.allInvalid;
                    this.perpHori[row][col]          = ScrabbleGame.allInvalid;
                    this.perpScoreDataVert[row][col] = ScrabbleGame.invalidPerpWordScore;
                    this.perpScoreDataHori[row][col] = ScrabbleGame.invalidPerpWordScore;

                    continue;
                }

                this.initializeValidPerpendicularPlacementAndScoringDataAt(row, col, true);
                this.initializeValidPerpendicularPlacementAndScoringDataAt(row, col, false);
            }
        }
    }

    private void initializeValidPerpendicularPlacementAndScoringDataAt(int row, int col, boolean isVertical)
    {
        boolean[][][] perpSource          = isVertical ? this.perpVert : this.perpHori;
        int[][]       perpScoreDataSource = isVertical ? this.perpScoreDataVert : this.perpScoreDataHori;
        int[][][]     scoreDataSource     = isVertical ? this.scoreDataHori : this.scoreDataVert;
        boolean[][]   canPlaceSource      = isVertical ? this.canPlaceVert : this.canPlaceHori;
        int           wordStartPerp       = isVertical ? col : row;
        int           lineBoundPerp       = isVertical ? this.gameState.getCols() : this.gameState.getRows();
        Offset        offsetPerp          = isVertical ? ScrabbleGame.horiOffset : ScrabbleGame.vertOffset;
        boolean       beforeEmpty         = wordStartPerp == 0 || this.gameState.isEmptyAt(offsetPerp.newRow(row, -1),
                                                                                           offsetPerp.newCol(col, -1)
        );
        boolean       afterEmpty          = wordStartPerp == lineBoundPerp - 1 ||
                                            this.gameState.isEmptyAt(offsetPerp.newRow(row, 1),
                                                                     offsetPerp.newCol(col, 1)
                                            );
        int           before              = wordStartPerp, after = wordStartPerp;
        int           i                   = 0;
        int           score               = 0;
        int           prefixScore         = 0;
        int           suffixScore         = 0;

        while (after + 1 < lineBoundPerp &&
               !this.gameState.isEmptyAt(offsetPerp.newRow(row, i + 1), offsetPerp.newCol(col, i + 1)))
        {
            int newRow = offsetPerp.newRow(row, i + 1), newCol = offsetPerp.newCol(col, i + 1);

            if (!this.gameState.isWildcardAt(newRow, newCol))
            {
                int s = this.letterScoreMap.getScore(this.gameState.getCharAt(newRow, newCol));
                score += s;
                suffixScore += s;
            }

            after++;
            i++;
        }

        i = 0;

        while (before - 1 >= 0 &&
               !this.gameState.isEmptyAt(offsetPerp.newRow(row, i - 1), offsetPerp.newCol(col, i - 1)))
        {
            int newRow = offsetPerp.newRow(row, i - 1), newCol = offsetPerp.newCol(col, i - 1);

            if (!this.gameState.isWildcardAt(newRow, newCol))
            {
                int s = this.letterScoreMap.getScore(this.gameState.getCharAt(newRow, newCol));
                score += s;
                prefixScore += s;
            }

            before--;
            i--;
        }

        if (beforeEmpty && afterEmpty)
        { // nothing before or after, anything is valid for placement
            canPlaceSource[row][col]      = true;
            perpSource[row][col]          = ScrabbleGame.allValid;
            perpScoreDataSource[row][col] = ScrabbleGame.invalidPerpWordScore;
            scoreDataSource[row][col]     = new int[]{
                this.gameState.getLetterMultiplierAt(row, col), this.gameState.getWordMultiplierAt(row, col),
                prefixScore, suffixScore
            };

            return;
        }

        WordGraphDictionary.WGNode currentPath = this.dictionary.getRoot();
        int                        current     = before;

        while (current < wordStartPerp)
        {
            currentPath = currentPath.getPath(
                this.gameState.getCharAt(offsetPerp.newRow(row, i), offsetPerp.newCol(col, i)));

            if (currentPath == null) break;

            current++;
            i++;
        }

        if (current == wordStartPerp)
        { // before tile is valid prefix
            boolean[] result = new boolean[IScrabbleGameState.alphaChars.length];
            int       scoreData;
            boolean   hasAny = false;

            for (Character c : currentPath.getPaths())
            {
                if (!this.possibleCharPlacements[ScrabbleUtil.charToInt(c)]) continue;

                WordGraphDictionary.WGNode afterPath = currentPath.getPath(c);
                current = wordStartPerp + 1;
                i       = 1;

                while (current <= after)
                {
                    afterPath = afterPath.getPath(
                        this.gameState.getCharAt(offsetPerp.newRow(row, i), offsetPerp.newCol(col, i)));

                    if (afterPath == null) break;

                    current++;
                    i++;
                }

                if (afterPath != null && afterPath.isWordHere())
                {
                    result[ScrabbleUtil.charToInt(c)] = true;
                    hasAny                            = true;
                }
            }

            if (hasAny)
            {
                canPlaceSource[row][col] = true;
                scoreData                = score;
            }
            else
            {
                scoreData = ScrabbleGame.invalidPerpWordScore;
            }

            perpSource[row][col]          = result;
            perpScoreDataSource[row][col] = scoreData;
        }
        else
        {
            perpSource[row][col]          = ScrabbleGame.allInvalid;
            perpScoreDataSource[row][col] = ScrabbleGame.invalidPerpWordScore;
        }

        scoreDataSource[row][col] = new int[]{
            this.gameState.getLetterMultiplierAt(row, col), this.gameState.getWordMultiplierAt(row, col), prefixScore,
            suffixScore
        };
    }

}
