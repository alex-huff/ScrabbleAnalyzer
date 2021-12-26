package com.alexfh.scrabbleai;

public record Move(String playedWord, char[] playedTiles, boolean isVertical, int row, int col) { }
