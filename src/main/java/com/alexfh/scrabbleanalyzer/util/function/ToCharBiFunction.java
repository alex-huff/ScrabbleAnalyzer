package com.alexfh.scrabbleanalyzer.util.function;

@FunctionalInterface
public interface ToCharBiFunction<T, U>
{

    char applyAsChar(T t, U u);

}