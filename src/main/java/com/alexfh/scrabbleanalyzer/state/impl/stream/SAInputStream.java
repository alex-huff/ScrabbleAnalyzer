package com.alexfh.scrabbleanalyzer.state.impl.stream;

import com.alexfh.scrabbleanalyzer.state.IPlayerTileRack;
import com.alexfh.scrabbleanalyzer.state.IScrabbleBoard;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;
import com.alexfh.scrabbleanalyzer.state.impl.PlayerTileRackImpl;
import com.alexfh.scrabbleanalyzer.state.impl.ScrabbleBoardImpl;
import com.alexfh.scrabbleanalyzer.state.impl.ScrabbleGameStateImpl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SAInputStream extends DataInputStream
{

    public SAInputStream(InputStream inputStream)
    {
        super(inputStream);
    }

    public IScrabbleGameState readGameState() throws IOException
    {
        return new ScrabbleGameStateImpl(
            this.readScrabbleBoard(),
            this.readPlayerTileRack()
        );
    }

    private IScrabbleBoard readScrabbleBoard() throws IOException
    {
        int rows = this.readInt();
        int cols = this.readInt();

        return new ScrabbleBoardImpl(
            rows,
            cols,
            this.read2DInt(rows, cols),
            this.read2DInt(rows, cols),
            this.read2DChar(rows, cols),
            this.read2DBoolean(rows, cols),
            this.readInt(),
            this.readInt()
        );
    }

    private IPlayerTileRack readPlayerTileRack() throws IOException
    {
        int size = this.readInt();

        return new PlayerTileRackImpl(
            size,
            this.read1DChar(size),
            this.readCharacterList()
        );
    }

    private List<Character> readCharacterList() throws IOException
    {
        int             size       = this.readInt();
        List<Character> characters = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
        {
            characters.add(this.readChar());
        }

        return characters;
    }

    private char[] read1DChar(int size) throws IOException
    {
        char[] chars = new char[size];

        for (int i = 0; i < size; i++)
        {
            chars[i] = this.readChar();
        }

        return chars;
    }

    private int[][] read2DInt(int rows, int cols) throws IOException
    {
        int[][] ints = new int[rows][cols];

        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                ints[r][c] = this.readInt();
            }
        }

        return ints;
    }

    private char[][] read2DChar(int rows, int cols) throws IOException
    {
        char[][] chars = new char[rows][cols];

        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                chars[r][c] = this.readChar();
            }
        }

        return chars;
    }

    private boolean[][] read2DBoolean(int rows, int cols) throws IOException
    {
        boolean[][] booleans = new boolean[rows][cols];

        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                booleans[r][c] = this.readBoolean();
            }
        }

        return booleans;
    }

}
