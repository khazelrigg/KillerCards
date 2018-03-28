package kam.hazelrigg;

import kam.hazelrigg.Cards.Card;
import kam.hazelrigg.Cards.Deck;

import java.util.*;


public class TienLen {
    private Scanner scanner = new Scanner(System.in);
    private int turnCount = 0;
    private int roundCount = 1;

    enum lastState {SINGLE, PAIR, TRIPLE, RUN}
    protected lastState roundState = lastState.SINGLE;

    private Player[] players = {new Player(1), new Player(2), new Player(3), new Player(4)};
    private Deck playedDeck = new Deck(0);
    protected Card lastCard;
    protected Player whoPlaying;
    private int skips = 0;

    TienLen() {
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
                if (isFirstTurn())
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
        whoPlaying = players[0];
        sortHand();
        printTurnInfo();
        System.out.println("First Turn, played your " + new Card(3, Card.Suit.SPADES).toString(true));

        playedDeck.addCard(new Card(3, Card.Suit.SPADES));
        lastCard = playedDeck.peek(0);
        askRoundState();
    }

    private void takeTurn(Player player) {
        whoPlaying = player;
        sortHand();

        if (skips >= 3) {
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
        /*
        TODO only display valid options
         */
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
        if (isFirstTurn()) return;
        int[] availableCards = getAvailableCards();
        Card playerCard = chooseCard(availableCards);
        if (playerCard != null) {
            lastCard = playerCard;
            playedDeck.addCard(playerCard);
        }
    }

    private void playPair() {
        //TODO redo this section to allow selection of a value opposed to selecting individual cards
        int[] availableCards = getAvailableCards();
        if (isFirstTurn()) {
            availableCards = getIndicesOfValues(3);
            Card card = chooseCard(availableCards);
            if (card != null) {
                playedDeck.addCard(card);
                lastCard = card;
            }
            return;
        }

        Card card1 = chooseCard(availableCards);
        if (card1 != null) {
            playedDeck.addCard(card1);
            lastCard = card1;
        }

        availableCards = getIndicesOfValues(card1.getValue());
        // card2 is card with same value
        Card card2 = chooseCard(availableCards);
        if (card2 != null) {
            playedDeck.addCard(card2);
            lastCard = card2;
        }
    }

    private void playTriple() {
        int[] availableCards = getAvailableCards();
        System.out.println("TRIPLES AVAILABLE - " + Arrays.toString(availableCards));
        for (int i = 0; i < 3; i++) {
            Card card = chooseCard(availableCards);
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
    private Card chooseCard(int[] available) {
        if (whoPlaying.getState() == Player.playerState.PASS) {
            return null;
        }
        if (available.length < 1) {
            skipPlayer();
            return null;
        }

        int enteredNum = promptForCard(available);
        boolean isSure = askVerifyTurnSelection(enteredNum);

        if (isSure) {
            return whoPlaying.getHand().drawCard(available[enteredNum - 1]);
        } else {
            chooseCard(available);
        }
        return null;
    }

    private int promptForCard(int[] available) {
        int enteredNum;

        while (true) {
            if (!isFirstTurn() || turnCount != 0 && roundCount > 1) {
                System.out.println(getStringAvailable(available) + " (99) Pass");
            } else {
                System.out.println(getStringAvailable(available));
            }
            enteredNum = askInt();
            scanner.nextLine();

            if (enteredNum == 99 && isFirstTurn()) {
                System.out.println("Can't pass on the first turn");
                System.out.println(getStringAvailable(available));
            } else if (enteredNum == 99 || enteredNum <= available.length) {
                break;
            } else {
                System.out.println("Try that again");
                System.out.println(getStringAvailable(available));
            }
        }
        return enteredNum;
    }


    private String getStringAvailable(int[] available) {
        //TODO remove useage of getAvailable
        StringBuilder txt = new StringBuilder("What card would you like to play\n");
        for (int i = 0; i < available.length; i++) {
            txt.append("(").append(i + 1).append(") ").append(whoPlaying.getHand().peek(available[i]).toString(true)).append("\t");
        }
        return txt.toString();
    }

    /**
     * Get a list of card indices that the player can play
     *
     * @return int array with indices
     */
    protected int[] getAvailableCards() {
        ArrayList<Integer> available = new ArrayList<>();

        if (roundState == lastState.PAIR) {
            return getPairIndices();
        }

        if (roundState == lastState.TRIPLE) {
            return getTripleIndices();
        }

        // For playing single cards
        for (int i = 0; i < getHandSize(); i++) {
            Card c = getPlayerCard(i);
            // Check if c is a greater card
            boolean isBigger = compareCards(c, lastCard);
            if (isBigger) {
                available.add(i);
            }
        }

        return available.stream().mapToInt(i -> i).toArray();
    }

    protected int[] getPairIndices() {
        ArrayList<Integer> pairValues = getValues(2);
        return getIndicesOfValues(pairValues);
    }

    protected int[] getTripleIndices() {
        ArrayList<Integer> tripleValues = getValues(3);
        return  getIndicesOfValues(tripleValues);
    }

    /**
     * Finds values in the players hand that appear at least n times
     * @param occurences Minimum occurences that a value must appear in players hand
     * @return List of values that are acceptable
     */
    protected ArrayList<Integer> getValues(int occurences) {
        //TODO make sure this also checks that card values are greater than last played card
        HashMap<Integer, Integer> valueCount = new HashMap<>();
        // Populate hashmap with each values number of occurences
        for (Card card : whoPlaying.getCards()) {
            int val = card.getValue();
            if (valueCount.containsKey(val)) {
                valueCount.put(val, valueCount.get(val) + 1);
            } else {
                valueCount.put(val, 1);
            }
        }

        // Add valid values with at least n occurences
        ArrayList<Integer> acceptableValues = new ArrayList<>();
        valueCount.forEach((k, v) -> {
            if (v >= occurences) {
                acceptableValues.add(k);
            }
        });
        return acceptableValues;
    }

    private int[] getIndicesOfValues(ArrayList<Integer> values) {
        ArrayList<Integer> indices = new ArrayList<>();

        for (int index = 0; index < whoPlaying.getHand().getSize(); index++) {
            int currentValue = whoPlaying.getHand().peek(index).getValue();

            if (values.contains(currentValue)) {
                indices.add(index);
            }
        }
        return indices.stream().mapToInt(i -> i).toArray();
    }

    private int[] getIndicesOfValues(int value) {
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < whoPlaying.getHand().getSize(); i++) {
            int currentValue = whoPlaying.getHand().peek(i).getValue();
            if (currentValue == value) {
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

    private String getDeckString(Deck d) {
        StringBuilder sb = new StringBuilder();
        for (Card c : d.getDeck()) {
            sb.append(c.toString(true)).append(" ");
        }
        return sb.toString();
    }

    private void skipPlayer() {
        System.out.println("Passing...");
        whoPlaying.setState(Player.playerState.PASS);
        skips++;
    }

    private void clearPlayerStates() {
        for (Player p : players) {
            // Players that are out are unable to continue playing, do not change their state back to normal
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

    private Card getPlayerCard(int i) {
        return whoPlaying.getHand().peek(i);
    }

    private int getHandSize() {
        return whoPlaying.getHand().getSize();
    }

    private void sortHand() {
        //https://stackoverflow.com/questions/2784514/sort-arraylist-of-custom-objects-by-property#2784576
        whoPlaying.getHand().getDeck().sort(Comparator.comparing(Card::getValue));
    }

    private boolean isFirstTurn() {
        return roundCount == 1 && turnCount == 0;
    }

}
