package com.alexfh.test.dictionary;

import static org.junit.jupiter.api.Assertions.*;

import com.alexfh.scrabbleai.ai.PermuteTree;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class PermuteTest {

    @Test
    public void testPermuteTree() {
        PermuteTree permuteTree = new PermuteTree(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' });
        AtomicInteger i = new AtomicInteger(-1); // -1 to ignore 0-perm
        Set<String> permutations = new TreeSet<>();

        permuteTree.forEach(
            perm -> {
                i.incrementAndGet();
                assertFalse(permutations.contains(perm));
                permutations.add(perm);
            }
        );
        assertEquals(i.get(), 13699);
        // 13699 = (7*6*5*4*3*2*1) + 7*(6*5*4*3*2*1) + 21*(5*4*3*2*1) + 35*(4*3*2*1) + 35*(3*2*1) + 21*(2*1) + 7
    }

}