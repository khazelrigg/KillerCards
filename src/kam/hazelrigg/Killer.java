package kam.hazelrigg;

import kam.hazelrigg.Cards.Card;
import kam.hazelrigg.Cards.Deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;


public class Killer {

    private Scanner scanner = new Scanner(System.in);
    private int turns = 0;
    private int roundCount = 0;

    enum lastState {SINGLE, PAIR, TRIPLE, RUN}
    private lastState roundState = lastState.SINGLE;

    private Player[] players = {new Player(1), new Player(2), new Player(3), new Player(4)};
    private Deck playedDeck = new Deck(0);
    private Card lastCard;
    private int runLen = 0;
    private Player whoPlaying;
    private int skips = 0;

    Killer() {
        dealCards();
    }

    private void dealCards() {
        Deck reg52 = new Deck();
        reg52.shuffleCards();

        while (reg52.getSize() != 0) {
            for (Player player : players) {
                player.addCard(reg52.drawCard());
            }
        }
    }

    public void play() {
        getFirstPlayer();
        whoPlaying = players[0];
        nextRound();
        while (true) {
            for (Player player : players) {
                if (player.getState() != Player.playerState.OUT && player.getState() != Player.playerState.PASS) {
                    whoPlaying = player;
                    takeTurn(player);
                } else {
                    System.out.println("\nSkipping player " + player.getPlayerNum()
                            + "... Moving to next player (P" + (player.getPlayerNum() + 1) + ")");
                }
            }
            if (isGameOver())
                break;
        }

    }

    private void nextRound() {
        setupRound();
        askRoundState();
    }

    private void setupRound() {
        clearPlayerStates();
        skips = 0;
        playedDeck = new Deck(0);
        roundCount++;
        lastCard = null;
        turns = 0;
    }
    private void askRoundState() {
        System.out.println("\n=======================[ Round " + (roundCount) + " ]=======================");
        System.out.println("Hand: " + getDeckString(whoPlaying.getHand()) + "\t\t | Size: " + whoPlaying.getHand().getSize());

        if (roundCount > 1) {
            System.out.println("Options: (S)ingle\t(P)air\t(T)riple\t(R)un");

            char i = askChar();
            if (i == 's') {
                roundState = lastState.SINGLE;
            } else if (i == 'p') {
                roundState = lastState.PAIR;
            } else if (i == 't') {
                roundState = lastState.TRIPLE;
            } else if (i == 'r') {
                roundState = lastState.RUN;
                runLen = getRunLength();
            }
        }
    }

    private void getFirstPlayer() {
        Player first = players[0];
        for (int p = 0; p < players.length; p++) {
            for (int i = 0; i < players[p].getHand().getSize(); i++) {
                Deck hand = players[p].getHand();

                if (hand.peek(i).getSuit() == Card.Suit.SPADES && hand.peek(i).getValue() == 3) {
                    playedDeck.addCard(hand.drawCard(i));
                    players[0] = players[p];
                    players[p] = first;
                }

            }
        }
        resetPlayerNums();
    }

    void resetPlayerNums() {
        for (int i = 0; i < 4; i++) {
            players[i].setPlayerNum(i + 1);
        }
    }

    int getRunLength() {
        while (true) {
            int wants = askInt("How many cards do you want to play in your run: ");
            scanner.nextLine();

            if (wants > 0 && wants <= getMaxRun()) {
                return wants;
            }
        }

    }

    int getMaxRun() {
        return whoPlaying.getHand().getSize();
    }

    private void takeTurn(Player player) {
        whoPlaying = player;
        boolean timeForNewRound = checkSkips();
        if (timeForNewRound) {
            nextRound();
            return;
        }

        printTurnInfo();
        takeCards();
        turns++;
    }

    private void takeCards() {
        switch (roundState) {
            case SINGLE:
                playSingle();
                return;
            case PAIR:
                playPair();
                return;
            case TRIPLE:
                playTriple();
                return;
            case RUN:
                playRun();
                break;
            default:
                break;
        }
    }

    private void playSingle() {
        if (turns == 0) {
            System.out.println(getStringAvailable());
            System.out.println("First Turn, playing your " + new Card(3, Card.Suit.SPADES).toString(true));
            playedDeck.addCard(new Card(3, Card.Suit.SPADES));
            return;
        }
        Card playerCard = chooseCard();
        if (playerCard != null) {
            playedDeck.addCard(playerCard);
        }
    }

    private void playPair() {
        for (int i = 0; i < 2; i++) {
            Card card = chooseCard();
            if (card != null) {
                playedDeck.addCard(card);
                lastCard = card;
            }
        }
    }

    private void playTriple() {
        for (int i = 0; i < 3; i++) {
            Card card = chooseCard();
            if (card != null) {
                playedDeck.addCard(card);
                lastCard = card;
            }
        }
    }

    private void playRun() {

    }

    /**
     * Present the player with a list of cards to play
     *
     * @return Card they choose
     */
    private Card chooseCard() {
        if (whoPlaying.getState() == Player.playerState.PASS)
            return null;

        int[] availableCards = getAvailableCards();

        switch (roundState) {
            case PAIR:
                if (!hasPairs()) {
                    skipPlayer();
                    return null;
                }
                break;
            case TRIPLE:
                if (!hasTriples()) {
                    skipPlayer();
                    return null;
                }
                break;
            default:
                if (availableCards.length < 1) {
                    skipPlayer();
                    return null;
                }
        }

        int enteredNum = promptForCard();
        boolean isSure = verifyUserSelection(enteredNum);

        if (isSure) {
            lastCard = whoPlaying.getHand().drawCard(availableCards[enteredNum - 1]);
        } else {
            chooseCard();
        }
        return lastCard;

    }

    void skipPlayer() {
        System.out.println("Passing...");
        whoPlaying.setState(Player.playerState.PASS);
        skips++;
    }

    boolean hasPairs() {
        return lastCard == null || getAvailableCards().length >= 2;
    }

    boolean hasTriples() {
        return getTriplePairIndices().length > 2;
    }

    int promptForCard() {
        int enteredNum;
        int[] availableCards = getAvailableCards();
        while (true) {
            System.out.println(getStringAvailable() + " (99) Pass");
            enteredNum = askInt("> ");
            scanner.nextLine();
            if (enteredNum == 99 || enteredNum <= availableCards.length) {
                break;
            } else {
                System.out.println("Try that again");
                System.out.println(getStringAvailable());
            }
        }
        return enteredNum;
    }

    boolean verifyUserSelection(int num) {
        boolean doubleCheck;

        if (num == 99) {
             doubleCheck = askYesNo("Are you sure you want to pass this round");
             if (doubleCheck) {
                 skipPlayer();
                 return false;
             }
        } else {
            doubleCheck = askYesNo("Are you sure you want to play your "
                    + whoPlaying.getHand().peek(getAvailableCards()[num - 1]).toString(true));
        }
        return doubleCheck;
    }


    private String getStringAvailable() {
        StringBuilder txt = new StringBuilder("What card would you like to play\n");
        for (int i = 0; i < getAvailableCards().length; i++) {
            txt.append("(").append(i + 1).append(") ").append(whoPlaying.getHand().peek(getAvailableCards()[i]).toString(true)).append("\t\t");
        }
        return txt.toString();
    }

    /**
     * Get a list of card indices that the player can play
     *
     * @return int array with indices
     */
    private int[] getAvailableCards() {
        Player player = whoPlaying;
        ArrayList<Integer> available = new ArrayList<>();

        for (int i = 0; i < player.getHand().getSize(); i++) {
            Card c = player.getHand().peek(i);
            if (roundCount == 0 && turns == 0 && c.getSuit() == Card.Suit.SPADES && c.getValue() == 3) {
                return new int[]{i};
            } else {
                if (roundState == lastState.PAIR) {
                    return getCardPairsIndices();
                }
                if (roundState == lastState.TRIPLE) {
                    return getTriplePairIndices();
                } else {
                    // Check if c is a greater card
                    boolean isBigger = compareCards(c, lastCard);
                    if (isBigger) {
                        available.add(i);
                    }
                }
            }
        }

        return available.stream().mapToInt(i -> i).toArray();
    }

    private int[] getCardPairsIndices() {

        if (lastCard != null) {
            return findPairsIndex();
        }

        ArrayList<Integer> pairsPossible = new ArrayList<>();
        whoPlaying.getHand().getDeck().forEach(e -> pairsPossible.add(e.getValue()));
        Collections.sort(pairsPossible);

        for (int i = 0; i < pairsPossible.size() - 1; i++) {
            if (!pairsPossible.get(i).equals(pairsPossible.get(i + 1))) {
                pairsPossible.remove(i);
                i--;
            }
        }
        pairsPossible.remove(pairsPossible.size() - 1);

        ArrayList<Integer> pairIndices = new ArrayList<>();
        for (int i = 0; i < whoPlaying.getHand().getSize(); i++) {
            int cardVal = whoPlaying.getHand().peek(i).getValue();
            if (pairsPossible.contains(cardVal) && !pairIndices.contains(cardVal)) {
                pairIndices.add(i);
            }
        }

        return pairIndices.stream().mapToInt(i -> i).toArray();
    }

    private int[] getTriplePairIndices() {

        if (lastCard != null) {
            return findPairsIndex();
        }

        ArrayList<Integer> triples = new ArrayList<>();
        whoPlaying.getHand().getDeck().forEach(e -> triples.add(e.getValue()));
        Collections.sort(triples);

        for (int i = 0; i < triples.size() - 2; i++) {
            if (triples.get(i).equals(triples.get(i + 1)) && !triples.get(i).equals(triples.get(i + 2))) {
                triples.remove(i);
                i--;
            }
        }
        triples.remove(triples.size() - 1);

        ArrayList<Integer> pairIndices = new ArrayList<>();
        for (int i = 0; i < whoPlaying.getHand().getSize(); i++) {
            int cardVal = whoPlaying.getHand().peek(i).getValue();
            if (triples.contains(cardVal)) {
                pairIndices.add(i);
            }
        }

        return pairIndices.stream().mapToInt(i -> i).toArray();
    }

    private int[] findPairsIndex() {
        ArrayList<Integer> indices = new ArrayList<>();
        int valToMatch = lastCard.getValue();
        for (int i = 0; i < whoPlaying.getHand().getSize(); i++) {
            if (whoPlaying.getHand().peek(i).getValue() == valToMatch) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(i -> i).toArray();
    }

    private void printTurnInfo() {
        System.out.println("\n=====================[ Turn " + (turns + 1) + " - (P" + whoPlaying.getPlayerNum() + ") ]=====================");
        if (playedDeck.getSize() > 0) {
            System.out.println("Played Last: " + getDeckString(playedDeck) + " | " + roundState);
        }
        System.out.println("Your Hand: " + getDeckString(whoPlaying.getHand()) + "\t\t | Size: " + whoPlaying.getHand().getSize());
    }

    private boolean compareCards(Card c, Card c2) {
        if (c2 == null) {
            return true;
        }

        if (getValue(c) == getValue(c2)) {
            return getSuitValue(c) > getSuitValue(c2);
        }

        return getValue(c) > getValue(c2);
    }

    private int getSuitValue(Card card) {
        switch (card.getSuit()) {
            case HEARTS:
                return 4;
            case DIAMONDS:
                return 3;
            case CLUBS:
                return 2;
            case SPADES:
                return 1;
            default:
                return 0;
        }
    }

    private int getValue(Card card) {
        switch (card.getValue()) {
            case 2:
                return 13;
            case 1:
                return 12;
            case 13:
                return 11;
            case 12:
                return 10;
            case 11:
                return 9;
            default:
                return card.getValue();
        }
    }

    private boolean checkSkips() {
        return skips == 3;
    }

    private int askInt(String msg) {
        int number;
        do {
            System.out.print(msg + " ");
            while (!scanner.hasNextInt()) {
                System.out.print("retry: " + msg + " ");
                scanner.next();
            }
            number = scanner.nextInt();
        } while (number <= 0);
        return number;
    }

    private char askChar() {
        char c = scanner.next(".").charAt(0);
        return c;
    }

    private boolean askYesNo(String msg) {
        System.out.print(msg + " [y/n]\n> ");
        String response = scanner.nextLine();
        if (response.length() > 0 && response.toLowerCase().charAt(0) == 'y')
            return true;
        return false;
    }

    private String getDeckString(Deck d) {
        StringBuilder sb = new StringBuilder();
        for (Card c : d.getDeck()) {
            sb.append(c.toString(true)).append(" ");
        }
        return sb.toString();
    }

    void clearPlayerStates() {
        for (Player p : players) {
            if (p.getState() != Player.playerState.OUT) {
                p.setState(Player.playerState.NORMAL);
            }
        }

    }

    boolean isGameOver() {
        for (Player p : players) {
            if (p.getHand().getSize() < 1) return true;
        }
        return false;
    }

}
