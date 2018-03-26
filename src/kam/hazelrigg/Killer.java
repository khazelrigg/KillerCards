package kam.hazelrigg;

import kam.hazelrigg.Cards.Card;
import kam.hazelrigg.Cards.Deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;


public class Killer {
    private Scanner scanner = new Scanner(System.in);
    private int turnCount = 0;
    private int roundCount = 0;

    enum lastState {SINGLE, PAIR, TRIPLE, RUN}

    private lastState roundState = lastState.SINGLE;

    private Player[] players = {new Player(1), new Player(2), new Player(3), new Player(4)};
    private Deck playedDeck = new Deck(0);
    private Card lastCard;
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
        while (true) {
            for (Player player : players) {
                if (turnCount == 0 && roundCount == 0)
                    takeFirstTurn();

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
        turnCount = 0;
    }


    private void takeFirstTurn() {
        roundCount++;
        whoPlaying = players[0];
        sortHand();
        printTurnInfo();
        System.out.println("First Turn, played your " + new Card(3, Card.Suit.SPADES).toString(true));

        playedDeck.addCard(new Card(3, Card.Suit.SPADES));
        lastCard = playedDeck.peek(0);

        askRoundState();
        turnCount++;
    }


    private void takeTurn(Player player) {
        whoPlaying = player;
        sortHand();

        boolean timeForNewRound = checkSkips();
        if (timeForNewRound) {
            nextRound();
            return;
        }

        printTurnInfo();
        playCards();
        turnCount++;

    }

    private void askRoundState() {
        System.out.println("\n=======================[ Round " + (roundCount) + " ]=======================");
        System.out.println("Hand: " + getDeckString(whoPlaying.getHand()) + "\t\t | Size: " + whoPlaying.getHand().getSize());
        //TODO only show valid options & autoselect if only one option
        System.out.println("Options: (S)ingle\t(P)air\t(T)riple\t(R)un");

        char roundSelection = promptRoundSelection();
        switch (roundSelection) {
            case 's':
                roundState = lastState.SINGLE;
                break;
            case 'p':
                roundState = lastState.PAIR;
                break;
            case 't':
                roundState = lastState.TRIPLE;
                break;
            case 'r':
                roundState = lastState.RUN;
                break;
        }

    }

    private void getFirstPlayer() {
        Player first = players[0];
        for (int p = 0; p < players.length; p++) {
            for (int i = 0; i < players[p].getHand().getSize(); i++) {
                Deck hand = players[p].getHand();

                if (hand.peek(i).getSuit() == Card.Suit.SPADES && hand.peek(i).getValue() == 3) {
                    hand.drawCard(i);
                    players[0] = players[p];
                    players[p] = first;
                }

            }
        }
        setPlayerNums();
    }

    private void printTurnInfo() {
        System.out.println("\n=====================[ Turn " + (turnCount + 1) + " - (P" + whoPlaying.getPlayerNum() + ") ]=====================");
        if (playedDeck.getSize() > 0) {
            System.out.println("Played Last: " + getDeckString(playedDeck) + "\t\t | " + roundState);
        }
        System.out.println("Your Hand: " + getDeckString(whoPlaying.getHand()) + "\t\t | Size: " + whoPlaying.getHand().getSize());
    }

    private void playCards() {
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
        Card playerCard = chooseCard();
        if (playerCard != null) {
            lastCard = playerCard;
            playedDeck.addCard(playerCard);
        }
    }

    private void playPair() {
        Card card1 = chooseCard();
        if (card1 != null) {
            playedDeck.addCard(card1);
            lastCard = card1;
        }
        if (turnCount == 0)
            return;

        Card card2 = chooseCard();
        if (card2 != null) {
            playedDeck.addCard(card2);
            lastCard = card2;
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
        if (whoPlaying.getState() == Player.playerState.PASS) {
            return null;
        }

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
        boolean isSure = askVerifyTurnSelection(enteredNum);

        if (isSure) {
            return whoPlaying.getHand().drawCard(availableCards[enteredNum - 1]);
        } else {
            chooseCard();
        }
        return null;
    }


    /**
     * Get a list of card indices that the player can play
     *
     * @return int array with indices
     */
    private int[] getAvailableCards() {
        ArrayList<Integer> pairIndices = new ArrayList<>();

        if (roundState == lastState.PAIR) {
            return getCardPairIndices();
        }

        // For playing single cards
        for (int i = 0; i < getHandSize(); i++) {
            Card c = getPlayerCard(i);
            if (roundState == lastState.TRIPLE) {
                return getTriplePairIndices();
            } else {
                // Check if c is a greater card
                boolean isBigger = compareCards(c, lastCard);
                if (isBigger) {
                    pairIndices.add(i);
                }
            }
        }

        return pairIndices.stream().mapToInt(i -> i).toArray();
    }

    private int[] getCardPairIndices() {
        ArrayList<Integer> pairIndices = new ArrayList<>();
        ArrayList<Integer> pairValues = getPairValues();

        for (int index = 0; index < whoPlaying.getHand().getSize(); index++) {
            int currentValue = whoPlaying.getHand().peek(index).getValue();

            if (pairValues.contains(currentValue)) {
                pairIndices.add(index);
            }
        }
        return pairIndices.stream().mapToInt(i -> i).toArray();
    }


    private int promptForCard() {
        int enteredNum;
        int[] availableCards = getAvailableCards();
        while (true) {
            if (!(roundCount == 1 && turnCount == 1) || turnCount != 0 && roundCount > 1) {
                System.out.println(getStringAvailable() + " (99) Pass");
            } else {
                System.out.println(getStringAvailable());
            }
            enteredNum = askInt();
            scanner.nextLine();

            if (enteredNum == 99 && turnCount == 0) {
                System.out.println("Can't pass on the first turn");
                System.out.println(getStringAvailable());
            } else if (enteredNum == 99 || enteredNum <= availableCards.length) {
                break;
            } else {
                System.out.println("Try that again");
                System.out.println(getStringAvailable());
            }
        }
        return enteredNum;
    }

    private ArrayList<Integer> getPairValues() {

        ArrayList<Integer> pairIndices = new ArrayList<>();
        for (Card c : whoPlaying.getCards()) {
            pairIndices.add(c.getValue());
        }
        Collections.sort(pairIndices);


        ArrayList<Integer> canPair = new ArrayList<>();

        for (int i = 0; i < pairIndices.size() - 1; i++) {
            if (pairIndices.get(i).equals(pairIndices.get(i + 1))) {
                canPair.add(pairIndices.get(i));
            }
        }

        return canPair;
    }
    
    private int[] getTriplePairIndices() {

        if (lastCard != null) {
            return findMatchingPairs();
        }

        ArrayList<Integer> triples = new ArrayList<>();
        whoPlaying.getHand().getDeck().forEach(e -> triples.add(e.getValue()));
        Collections.sort(triples);

        for (int i = 0; i < triples.size() - 2; i++) {
            if (triples.get(i).equals(triples.get(i + 1)) && !triples.get(i).equals(triples.get(i + 2))) {
                triples.remove(i);
            }
        }
        triples.remove(triples.size() - 1);

        ArrayList<Integer> pairIndices = new ArrayList<>();
        for (int i = 0; i < getHandSize(); i++) {
            int cardVal = getPlayerCard(i).getValue();
            if (triples.contains(cardVal)) {
                pairIndices.add(i);
            }
        }

        return pairIndices.stream().mapToInt(i -> i).toArray();
    }

    private int[] findMatchingPairs() {
        ArrayList<Integer> indices = new ArrayList<>();

        int pairValue = lastCard.getValue();
        for (int i = 0; i < getHandSize(); i++) {
            if (whoPlaying.getHand().peek(i).getValue() == pairValue) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(i -> i).toArray();
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

    private int askInt() {
        int number;
        do {
            System.out.print("> ");
            while (!scanner.hasNextInt()) {
                System.out.print("retry: > ");
                scanner.next();
            }
            number = scanner.nextInt();
        } while (number <= 0);
        return number;
    }

    private char promptRoundSelection() {
        String acceptable = "sptr";
        while (true) {
            System.out.print("> ");
            String response = scanner.nextLine().toLowerCase();
            String c = response.substring(0, 1);
            if (acceptable.contains(c)) {
                return response.charAt(0);
            } else {
                System.out.print("retry: ");
            }
        }
    }

    private boolean askVerifyTurnSelection(int num) {
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

    private boolean askYesNo(String msg) {
        System.out.print(msg + " [y/n]\n" + "> ");
        String response = scanner.nextLine();
        return response.length() > 0 && response.toLowerCase().charAt(0) == 'y';
    }

    private void skipPlayer() {
        System.out.println("Passing...");
        whoPlaying.setState(Player.playerState.PASS);
        skips++;
    }


    private boolean hasPairs() {
        return lastCard == null || getAvailableCards().length >= 2;
    }

    private boolean hasTriples() {
        return getTriplePairIndices().length > 2;
    }

    private String getDeckString(Deck d) {
        StringBuilder sb = new StringBuilder();
        for (Card c : d.getDeck()) {
            sb.append(c.toString(true)).append(" ");
        }
        return sb.toString();
    }

    private void clearPlayerStates() {
        for (Player p : players) {
            if (p.getState() != Player.playerState.OUT) {
                p.setState(Player.playerState.NORMAL);
            }
        }

    }

    private void setPlayerNums() {
        for (int i = 0; i < 4; i++) {
            players[i].setPlayerNum(i + 1);
        }
    }

    private boolean isGameOver() {
        for (Player p : players) {
            if (p.getHand().getSize() < 1) return true;
        }
        return false;
    }

    private String getStringAvailable() {
        StringBuilder txt = new StringBuilder("What card would you like to play\n");
        for (int i = 0; i < getAvailableCards().length; i++) {
            txt.append("(").append(i + 1).append(") ").append(whoPlaying.getHand().peek(getAvailableCards()[i]).toString(true)).append("\t");
        }
        return txt.toString();
    }

    private Card getPlayerCard(int i) {
        return whoPlaying.getHand().peek(i);
    }

    private int getHandSize() {
        return whoPlaying.getHand().getSize();
    }

    private boolean checkSkips() {
        return skips == 3;
    }

    private void sortHand() {
        //https://stackoverflow.com/questions/2784514/sort-arraylist-of-custom-objects-by-property#2784576
        whoPlaying.getHand().getDeck().sort(Comparator.comparing(Card::getValue));
    }

}
