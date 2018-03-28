package kam.hazelrigg;

import kam.hazelrigg.Cards.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TienLenTest {
    private static TienLen testGame = new TienLen();

    @BeforeEach
    void setupGame() {
        testGame.whoPlaying = new Player(1);
        testGame.lastCard = new Card(3, Card.Suit.SPADES);
        giveWhoPlayingCards();
    }

    @Test
    void getsPairValues() {
        int[] values = testGame.getValues(2).stream().mapToInt(i -> i).toArray();
        int[] expected = {2, 4};
        assertTrue(Arrays.equals(values, expected));
    }

    @Test
    void getsPairIndices() {
        int[] pairs = testGame.getPairIndices();
        int[] expected = {0, 2, 3, 4, 5};
        assertTrue(Arrays.equals(pairs, expected));
    }

    @Test
    void getsTripleValues() {
        int[] values = testGame.getValues(3).stream().mapToInt(i -> i).toArray();
        int[] expected = {4};
        assertTrue(Arrays.equals(values, expected));
    }

    @Test
    void getsTripleIndices() {
        int[] triples = testGame.getTripleIndices();
        int[] expected = {2, 4, 5};
        assertTrue(Arrays.equals(triples, expected));
    }

    @Test
    void getsAvailableCardsSingle() {
        testGame.roundState = TienLen.lastState.SINGLE;
        testGame.lastCard = new Card(6, Card.Suit.SPADES);
        int[] available = testGame.getAvailableCards();
        System.out.println(Arrays.toString(available));
        int[] expectedIndices = {0, 3, 6};
        assertTrue(Arrays.equals(available, expectedIndices));
    }


    @Test
    void getsAvailableCardsPairs() {
        testGame.roundState = TienLen.lastState.PAIR;
        int[] available = testGame.getAvailableCards();
        int[] expectedPairIndices = {0, 2, 3, 4, 5};
        assertTrue(Arrays.equals(available, expectedPairIndices));
    }

    @Test
    void getsAvailableCardsTriples() {
        testGame.roundState = TienLen.lastState.TRIPLE;
        int[] available = testGame.getAvailableCards();
        int[] expectedTripleIndices = {2, 4, 5};
        assertTrue(Arrays.equals(available, expectedTripleIndices));
    }

    private void giveWhoPlayingCards() {
        testGame.whoPlaying.getHand().addAllCards(
                new Card(2, Card.Suit.SPADES),
                new Card(3, Card.Suit.SPADES),
                new Card(4, Card.Suit.HEARTS),
                new Card(2, Card.Suit.CLUBS),
                new Card(4, Card.Suit.DIAMONDS),
                new Card(4, Card.Suit.CLUBS),
                new Card(8, Card.Suit.SPADES));
    }

}
