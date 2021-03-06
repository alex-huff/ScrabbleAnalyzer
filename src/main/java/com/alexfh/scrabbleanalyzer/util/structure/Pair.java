package com.alexfh.scrabbleanalyzer.util.structure;

public
interface Pair<L, R>
{

    L getLeft();

    R getRight();

    void setLeft(L left);

    void setRight(R right);

}
