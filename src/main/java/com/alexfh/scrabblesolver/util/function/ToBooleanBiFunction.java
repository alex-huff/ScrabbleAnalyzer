package com.alexfh.scrabblesolver.util.function;

@FunctionalInterface
public interface ToBooleanBiFunction<T, U> {

    boolean applyAsBoolean(T t, U u);

}
