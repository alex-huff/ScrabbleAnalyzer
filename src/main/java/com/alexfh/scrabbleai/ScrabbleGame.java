package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.dictionary.IDictionary;
import com.alexfh.scrabbleai.rule.ILetterScoreMap;
import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScrabbleGame {

    private static final boolean[] allValid = new boolean[ScrabbleUtil.alphaChars.length];
    private static final boolean[] allInvalid = new boolean[ScrabbleUtil.alphaChars.length];

    static {
        Arrays.fill(ScrabbleGame.allValid, true);
    }

    private final ILetterScoreMap letterScoreMap;
    private final IDictionary dictionary;
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

    public ScrabbleGame(ILetterScoreMap letterScoreMap, IDictionary dictionary, IScrabbleBoard board, char[] playerTiles) {
        this.letterScoreMap = letterScoreMap;
        this.dictionary = dictionary;
        this.board = board;
        this.playerTiles = playerTiles;
        this.perpVert = new boolean[this.board.getRows()][this.board.getCols()][];
        this.perpHori = new boolean[this.board.getRows()][this.board.getCols()][];
    }

    public List<Move> findMoves() {
        ScrabbleUtil.timeIt(this::initializeValidPerpendicularPlacements, "initializeValidPerpendicularPlacements");

        List<Move> ret = new ArrayList<>();

        return ret;
    }

    private boolean[] determineValidChars(String perpWord) {
        boolean[] result = new boolean[ScrabbleUtil.alphaChars.length];

        for (int i = 0; i < ScrabbleUtil.alphaChars.length; i++) {
            result[i] = this.dictionary.hasWord(perpWord.replace(' ', ScrabbleUtil.alphaChars[i]));
        }

        return result;
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
                } else {
                    int left = col, right = col;

                    while (left - 1 >= 0 && !this.board.isEmptyAt(row, left - 1))
                        left--;

                    while (right + 1 < this.board.getCols() && !this.board.isEmptyAt(row, right + 1))
                        right++;

                    StringBuilder builder = new StringBuilder();
                    int current = left;

                    while (current <= right) {
                        builder.append(this.board.isEmptyAt(row, current) ? ' ' : this.board.getCharAt(row, current));

                        current++;
                    }

                    this.perpVert[row][col] = this.determineValidChars(builder.toString());
                }

                if (topEmpty && bottomEmpty) { // nothing above or below, anything is valid for horizontal placement
                    this.perpHori[row][col] = ScrabbleGame.allValid;
                } else {
                    int top = row, bottom = row;

                    while (top - 1 >= 0 && !this.board.isEmptyAt(top - 1, col))
                        top--;

                    while (bottom + 1 < this.board.getRows() && !this.board.isEmptyAt(bottom + 1, col))
                        bottom++;

                    StringBuilder builder = new StringBuilder();
                    int current = top;

                    while (current <= bottom) {
                        builder.append(this.board.isEmptyAt(current, col) ? ' ' : this.board.getCharAt(current, col));

                        current++;
                    }

                    this.perpHori[row][col] = this.determineValidChars(builder.toString());
                }
            }
        }
    }

}
