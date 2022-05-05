package com.alexfh.scrabbleanalyzer.util.function;

@FunctionalInterface
public interface ToBooleanBiFunction<T, U>
{

    boolean applyAsBoolean(T t, U u);

}
