package com.alexfh.scrabbleanalyzer.state.impl.stream;

import com.alexfh.scrabbleanalyzer.state.IPlayerTileRack;
import com.alexfh.scrabbleanalyzer.state.IScrabbleBoard;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;
import com.alexfh.scrabbleanalyzer.util.function.ToBooleanBiFunction;
import com.alexfh.scrabbleanalyzer.util.function.ToCharBiFunction;
import com.alexfh.scrabbleanalyzer.util.function.ToCharFunction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.ToIntBiFunction;

public
class SAOutputStream extends DataOutputStream
{

    public
    SAOutputStream(OutputStream inputStream)
    {
        super(inputStream);
    }

    public
    void writeScrabbleGameState(IScrabbleGameState gameState) throws IOException
    {
        this.writeScrabbleBoard(gameState);
        this.writePlayerTileRack(gameState);
    }

    private
    void writeScrabbleBoard(IScrabbleBoard board) throws IOException
    {
        int rows = board.getRows();
        int cols = board.getCols();

        this.writeInt(rows);
        this.writeInt(cols);
        this.write2DInt(board::getLetterMultiplierAt, rows, cols);
        this.write2DInt(board::getWordMultiplierAt, rows, cols);
        this.write2DChar(board::getCharAt, rows, cols);
        this.write2DBoolean(board::isWildcardAt, rows, cols);
        this.writeInt(board.getAnchorRow());
        this.writeInt(board.getAnchorCol());
    }

    private
    void writePlayerTileRack(IPlayerTileRack rack) throws IOException
    {
        int size = rack.getRackSize();

        this.writeInt(size);
        this.write1DChar(rack::getTileInRackAt, size);
        this.writeCharacterList(rack.getTilesInRack());
    }

    private
    void writeCharacterList(List<Character> characters) throws IOException
    {
        int size = characters.size();

        this.writeInt(size);

        for (Character character : characters)
        {
            this.writeChar(character);
        }
    }

    private
    void write1DChar(ToCharFunction<Integer> charAt, int size) throws IOException
    {
        for (int i = 0; i < size; i++)
        {
            this.writeChar(charAt.applyAsChar(i));
        }
    }

    private
    void write2DInt(ToIntBiFunction<Integer, Integer> intAt, int rows, int cols) throws IOException
    {
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                this.writeInt(intAt.applyAsInt(r, c));
            }
        }
    }

    private
    void write2DChar(ToCharBiFunction<Integer, Integer> charAt, int rows, int cols) throws IOException
    {
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                this.writeChar(charAt.applyAsChar(r, c));
            }
        }
    }

    private
    void write2DBoolean(ToBooleanBiFunction<Integer, Integer> booleanAt, int rows, int cols) throws IOException
    {
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                this.writeBoolean(booleanAt.applyAsBoolean(r, c));
            }
        }
    }

}
