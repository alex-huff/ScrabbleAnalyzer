package com.alexfh.scrabbleanalyzer.util.function;

@FunctionalInterface
public interface ToCharFunction<T>
{

    char applyAsChar(T t);

}
