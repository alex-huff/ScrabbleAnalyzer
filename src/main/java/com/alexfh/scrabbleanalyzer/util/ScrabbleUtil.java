package com.alexfh.scrabbleanalyzer.util;

import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;
import com.alexfh.scrabbleanalyzer.util.structure.ImmutablePair;
import com.alexfh.scrabbleanalyzer.util.structure.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

public class ScrabbleUtil
{

    public static final String newLineRegex = "\\R";

    public static void checkInterrupted() throws InterruptedException
    {
        if (Thread.interrupted()) throw new InterruptedException();
    }

    @SuppressWarnings("unused")
    public static <T> Pair<T, Long> getTimeToRetrieve(Supplier<T> supplier)
    {
        long start = System.nanoTime();

        T t = supplier.get();

        long finish = System.nanoTime();

        return new ImmutablePair<>(t, finish - start);
    }

    @FunctionalInterface
    public interface InterruptableSupplier<T>
    {

        T get() throws InterruptedException;

    }

    @FunctionalInterface
    public interface InterruptableRunnable
    {

        void run() throws InterruptedException;

    }

    public static <T> T timeRetrievalInterruptable(InterruptableSupplier<T> supplier, String message)
        throws InterruptedException
    {
        long start = System.nanoTime();

        T t = supplier.get();

        long finish = System.nanoTime();

        System.out.println(message + " " + 1.0D * (finish - start) / 1000000000 + "s");

        return t;
    }

    public static <T> T timeRetrieval(Supplier<T> supplier, String message)
    {
        long start = System.nanoTime();

        T t = supplier.get();

        long finish = System.nanoTime();

        System.out.println(message + " " + 1.0D * (finish - start) / 1000000000 + "s");

        return t;
    }

    public static void timeItInterruptable(InterruptableRunnable runnable, String message) throws InterruptedException
    {
        long start = System.nanoTime();

        runnable.run();

        long finish = System.nanoTime();

        System.out.println(message + " " + 1.0D * (finish - start) / 1000000000 + "s");
    }

    public static void timeIt(Runnable runnable, String message)
    {
        long start = System.nanoTime();

        runnable.run();

        long finish = System.nanoTime();

        System.out.println(message + " " + 1.0D * (finish - start) / 1000000000 + "s");
    }

    public static boolean isValidTiles(String s)
    {
        return s.matches("^[a-zA-Z" + IScrabbleGameState.wildCardTile + IScrabbleGameState.emptyMarker + "]*$");
    }

    public static int charToInt(char c)
    {
        return c - 97;
    }

    @SuppressWarnings("unused")
    public static char intToChar(int i)
    {
        return (char) (i + 97);
    }

    public static char[][] readRectangularBoardText(File file) throws IOException
    {
        String boardText = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        if (boardText.isEmpty() || boardText.isBlank()) throw new IllegalStateException("Empty board");

        String[] lines      = boardText.split(newLineRegex);
        int      rows       = lines.length;
        int      cols       = lines[0].length();
        char[][] boardChars = new char[rows][cols];

        for (int r = 0; r < rows; r++)
        {
            String line = lines[r].strip();

            if (line.length() != cols) throw new IllegalStateException("Non-rectangular board");

            boardChars[r] = line.toCharArray();
        }

        return boardChars;
    }

}
