package com.alexfh.scrabblesolver.state;

public interface IScrabbleGameState extends IScrabbleBoard, IPlayerTileRack {

    char emptyMarker = '.';
    char wildCardMarker = '0';
    char wildCardTile = '{';
    String alpha = "abcdefghijklmnopqrstuvwxyz";
    char[] alphaChars = alpha.toCharArray();

    IScrabbleGameState copyScrabbleGame();

    default boolean isEqualTo(IScrabbleGameState otherGameState) {
        return this.isEqualTo((IScrabbleBoard) otherGameState) &&
            this.isEqualTo((IPlayerTileRack) otherGameState);
    }

}
