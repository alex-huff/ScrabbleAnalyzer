package com.alexfh.scrabblesolver.util.function;

@FunctionalInterface
public interface ToCharBiFunction<T, U> {

    char applyAsChar(T t, U u);

}