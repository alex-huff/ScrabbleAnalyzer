package com.alexfh.scrabblesolver.state.impl;

import com.alexfh.scrabblesolver.state.IPlayerTileRack;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class ScrabbleGameStateImpl implements IScrabbleGameState {

    public static IScrabbleGameState defaultBlankScrabbleGameState() {
        return new ScrabbleGameStateImpl(ScrabbleBoardImpl.defaultBlankBoard(), PlayerTileRackImpl.defaultBlankRack());
    }

    public static IScrabbleGameState fromFiles(File gameFile, File multipliersFile, File rackFile) throws IOException {
        return new ScrabbleGameStateImpl(
            ScrabbleBoardImpl.fromFiles(gameFile, multipliersFile),
            PlayerTileRackImpl.fromFile(rackFile)
        );
    }

    private final IScrabbleBoard board;
    private final IPlayerTileRack rack;

    public ScrabbleGameStateImpl(IScrabbleBoard board, IPlayerTileRack rack) {
        this.board = board;
        this.rack = rack;
    }

    @Override
    public int getRackSize() {
        return this.rack.getRackSize();
    }

    @Override
    public int getNumTilesInRack() {
        return this.rack.getNumTilesInRack();
    }

    @Override
    public char getTileInRackAt(int i) {
        return this.rack.getTileInRackAt(i);
    }

    @Override
    public void setTileInRackAt(int i, char c) {
        this.rack.setTileInRackAt(i, c);
    }

    @Override
    public List<Character> getTilesInRack() {
        return this.rack.getTilesInRack();
    }

    @Override
    public IPlayerTileRack copyRack() {
        return this.rack.copyRack();
    }

    @Override
    public int getRows() {
        return this.board.getRows();
    }

    @Override
    public int getCols() {
        return this.board.getCols();
    }

    @Override
    public int getLetterMultiplierAt(int r, int c) {
        return this.board.getLetterMultiplierAt(r, c);
    }

    @Override
    public int getWordMultiplierAt(int r, int c) {
        return this.board.getWordMultiplierAt(r, c);
    }

    @Override
    public char getCharAt(int r, int c) {
        return this.board.getCharAt(r, c);
    }

    @Override
    public void setCharAt(int r, int c, char newChar) {
        this.board.setCharAt(r, c, newChar);
    }

    @Override
    public void removeCharAt(int r, int c) {
        this.board.removeCharAt(r, c);
    }

    @Override
    public boolean isWildcardAt(int r, int c) {
        return this.board.isWildcardAt(r, c);
    }

    @Override
    public void setWildcardAt(int r, int c, boolean isWild) {
        this.board.setWildcardAt(r, c, isWild);
    }

    @Override
    public int getAnchorRow() {
        return this.board.getAnchorRow();
    }

    @Override
    public int getAnchorCol() {
        return this.board.getAnchorCol();
    }

    @Override
    public IScrabbleBoard copyBoard() {
        return this.board.copyBoard();
    }

    @Override
    public IScrabbleGameState copyScrabbleGame() {
        return new ScrabbleGameStateImpl(this.copyBoard(), this.copyRack());
    }

}
