package com.alexfh.scrabblesolver.util.function;

@FunctionalInterface
public interface ToCharFunction<T> {

    char applyAsChar(T t);

}
