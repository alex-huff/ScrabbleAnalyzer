package com.alexfh.scrabbleanalyzer.state;

import com.alexfh.scrabbleanalyzer.gui.action.RevertableAction;

import java.util.function.BiPredicate;
import java.util.stream.IntStream;

public interface IScrabbleBoard
{

    int getRows();

    int getCols();

    int getLetterMultiplierAt(int r, int c);

    int getWordMultiplierAt(int r, int c);

    char getCharAt(int r, int c);

    RevertableAction setCharAt(int r, int c, char newChar);

    RevertableAction removeCharAt(int r, int c);

    boolean isWildcardAt(int r, int c);

    RevertableAction setWildcardAt(int r, int c, boolean isWild);

    default boolean isEmptyAt(int r, int c)
    {
        return this.getCharAt(r, c) == IScrabbleGameState.emptyMarker;
    }

    int getAnchorRow();

    int getAnchorCol();

    IScrabbleBoard copyBoard();

    default boolean isEqualTo(IScrabbleBoard otherBoard)
    {
        return
            this.getRows() == otherBoard.getRows() &&
            this.getCols() == otherBoard.getCols() &&
            this.trueForAllTiles(
                (r, c) -> this.getLetterMultiplierAt(r, c) == otherBoard.getLetterMultiplierAt(r, c)) &&
            this.trueForAllTiles((r, c) -> this.getWordMultiplierAt(r, c) == otherBoard.getWordMultiplierAt(r, c)) &&
            this.trueForAllTiles((r, c) -> this.getCharAt(r, c) == otherBoard.getCharAt(r, c)) &&
            this.trueForAllTiles((r, c) -> this.isWildcardAt(r, c) == otherBoard.isWildcardAt(r, c)) &&
            this.getAnchorRow() == otherBoard.getAnchorRow() &&
            this.getAnchorCol() == otherBoard.getAnchorCol();
    }

    private boolean trueForAllTiles(BiPredicate<Integer, Integer> tileCondition)
    {
        return IntStream.range(
            0,
            this.getRows()
        ).allMatch(
            r -> IntStream.range(
                0,
                this.getCols()
            ).allMatch(
                c -> tileCondition.test(r, c)
            )
        );
    }

}
