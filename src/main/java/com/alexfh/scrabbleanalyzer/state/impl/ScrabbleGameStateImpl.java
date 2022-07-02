package com.alexfh.scrabbleanalyzer.state.impl;

import com.alexfh.scrabbleanalyzer.gui.action.RevertibleAction;
import com.alexfh.scrabbleanalyzer.state.IPlayerTileRack;
import com.alexfh.scrabbleanalyzer.state.IScrabbleBoard;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public
class ScrabbleGameStateImpl implements IScrabbleGameState
{

    public static
    IScrabbleGameState defaultBlankScrabbleGameState()
    {
        return new ScrabbleGameStateImpl(ScrabbleBoardImpl.defaultBlankBoard(), PlayerTileRackImpl.defaultBlankRack());
    }

    public static
    IScrabbleGameState fromFiles(File gameFile, File multipliersFile, File rackFile) throws IOException
    {
        return new ScrabbleGameStateImpl(ScrabbleBoardImpl.fromFiles(gameFile, multipliersFile),
            PlayerTileRackImpl.fromFile(rackFile));
    }

    private final IScrabbleBoard  board;
    private final IPlayerTileRack rack;

    public
    ScrabbleGameStateImpl(IScrabbleBoard board, IPlayerTileRack rack)
    {
        this.board = board;
        this.rack  = rack;
    }

    @Override
    public
    int getRackSize()
    {
        return this.rack.getRackSize();
    }

    @Override
    public
    int getNumTilesInRack()
    {
        return this.rack.getNumTilesInRack();
    }

    @Override
    public
    char getTileInRackAt(int i)
    {
        return this.rack.getTileInRackAt(i);
    }

    @Override
    public
    RevertibleAction setTileInRackAt(int i, char c)
    {
        return this.rack.setTileInRackAt(i, c);
    }

    @Override
    public
    List<Character> getTilesInRack()
    {
        return this.rack.getTilesInRack();
    }

    @Override
    public
    IPlayerTileRack copyRack()
    {
        return this.rack.copyRack();
    }

    @Override
    public
    int getRows()
    {
        return this.board.getRows();
    }

    @Override
    public
    int getCols()
    {
        return this.board.getCols();
    }

    @Override
    public
    int getLetterMultiplierAt(int r, int c)
    {
        return this.board.getLetterMultiplierAt(r, c);
    }

    @Override
    public
    int getWordMultiplierAt(int r, int c)
    {
        return this.board.getWordMultiplierAt(r, c);
    }

    @Override
    public
    char getCharAt(int r, int c)
    {
        return this.board.getCharAt(r, c);
    }

    @Override
    public
    RevertibleAction setCharAt(int r, int c, char newChar)
    {
        return this.board.setCharAt(r, c, newChar);
    }

    @Override
    public
    RevertibleAction removeCharAt(int r, int c)
    {
        return this.board.removeCharAt(r, c);
    }

    @Override
    public
    boolean isWildcardAt(int r, int c)
    {
        return this.board.isWildcardAt(r, c);
    }

    @Override
    public
    RevertibleAction setWildcardAt(int r, int c, boolean isWild)
    {
        return this.board.setWildcardAt(r, c, isWild);
    }

    @Override
    public
    int getAnchorRow()
    {
        return this.board.getAnchorRow();
    }

    @Override
    public
    int getAnchorCol()
    {
        return this.board.getAnchorCol();
    }

    @Override
    public
    IScrabbleBoard copyBoard()
    {
        return this.board.copyBoard();
    }

    @Override
    public
    IScrabbleGameState copyScrabbleGame()
    {
        return new ScrabbleGameStateImpl(this.copyBoard(), this.copyRack());
    }

}
