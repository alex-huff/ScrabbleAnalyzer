package com.alexfh.test.dictionary;

import static org.junit.jupiter.api.Assertions.*;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.dictionary.WordGraphDictionary;
import com.alexfh.scrabblesolver.rule.ILetterScoreMap;
import com.alexfh.scrabblesolver.rule.impl.LetterScoreMapImpl;
import com.alexfh.scrabblesolver.state.impl.ScrabbleGameStateImpl;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ScrabbleGameTest {

    @Test
    public void testDistributionForAllGames() throws IOException, InterruptedException {
        ILetterScoreMap scoreMap = LetterScoreMapImpl.fromFile(
            new File("src/main/resources/scoremap.txt")
        );
        WordGraphDictionary dictionary = WordGraphDictionary.fromFile(
            new File("src/main/resources/nwl20.txt")
        );
        this.testDistribution(
            1,
            new int[] { 0, 114, 158, 133, 268, 159, 115, 115, 82, 74, 38, 40, 23, 19, 29, 14, 11, 3, 11 },
            dictionary,
            scoreMap
        );
        this.testDistribution(
            2,
            new int[] { 0, 0, 8, 18, 13, 15, 9, 20, 8, 10, 12, 12, 5, 9, 16, 2, 1, 3, 2, 2, 1, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, 1 },
            dictionary,
            scoreMap
        );
        this.testDistribution(
            3,
            new int[] { 0, 0, 4, 7, 11, 11, 7, 9, 5, 2, 4, 5, 5, 4, 1, 0, 1, 0, 0, 0, 1, 1 },
            dictionary,
            scoreMap
        );
        this.testDistribution(
            4,
            new int[] { 0, 0, 16, 50, 74, 78, 46, 18, 4, 0, 0, 0, 4, 0, 12, 0, 8 },
            dictionary,
            scoreMap
        );
        this.testDistribution(
            5,
            new int[] { 0, 0, 14, 13, 35, 46, 65, 65, 57, 35, 55, 57, 51, 28, 24, 17, 17, 10, 12, 10, 6, 3, 1, 3, 1, 0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 0, 0, 0, 0, 3 },
            dictionary,
            scoreMap
        );
    }

    public void testDistribution(int gameNum, int[] expectedDistribution, WordGraphDictionary dictionary, ILetterScoreMap scoreMap) throws IOException, InterruptedException {
        String gameFolder = "src/main/resources/games/game" + gameNum + "/";
        ScrabbleGame scrabbleGame = new ScrabbleGame(
            scoreMap,
            dictionary,
            ScrabbleGameStateImpl.fromFiles(
                new File(gameFolder + "board.txt"),
                new File("src/main/resources/multipliers.txt"),
                new File(gameFolder + "currentletters.txt")
            )
        );
        List<ScrabbleGame.Move> moves = ScrabbleUtil.timeRetrievalInterruptable(scrabbleGame::findMoves, "findMoves");

        ScrabbleUtil.timeIt(() -> Collections.sort(moves), "sort");

        int highestScore = moves.get(0).score();

        assertEquals(highestScore, expectedDistribution.length - 1);

        int[] scoreDistribution = new int[highestScore + 1];

        for (ScrabbleGame.Move move : moves) {
            scoreDistribution[move.score()]++;
        }

        assertArrayEquals(scoreDistribution, expectedDistribution);
    }

}
