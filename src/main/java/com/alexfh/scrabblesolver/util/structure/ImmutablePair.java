package com.alexfh.scrabblesolver.util.structure;

public record ImmutablePair<L, R>(L left, R right) implements Pair<L, R> {

    @Override
    public L getLeft() {
        return this.left;
    }

    @Override
    public R getRight() {
        return this.right;
    }

    @Override
    public void setLeft(L left) {
        throw new UnsupportedOperationException("Cannot modify an immutable pair");
    }

    @Override
    public void setRight(R right) {
        throw new UnsupportedOperationException("Cannot modify an immutable pair");
    }

}
