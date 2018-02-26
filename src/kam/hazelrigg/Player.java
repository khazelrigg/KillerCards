package kam.hazelrigg;

import kam.hazelrigg.Cards.Card;
import kam.hazelrigg.Cards.Deck;

import java.util.Comparator;
import java.util.Scanner;

public class Player {
    private Deck hand = new Deck(0);
    private int playerNum;
    public enum playerState {NORMAL, PASS, OUT};
    private playerState state = playerState.NORMAL;

    Player(int i) {
        playerNum = i;
    }

    @Override
    public String toString() {
        return hand.toString();
    }

    void addCard(Card c) {
        hand.addCard(c);
    }

    int getPlayerNum() {
        return playerNum;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public void setState(playerState state) {
        this.state = state;
    }

    public playerState getState() {
        return state;
    }

    public boolean handContains(Card c) {
        for (Card card : hand.getDeck()) {
            if (card == c) return true;
        }
        return false;
    }

    public Deck getHand() {
        return hand;
    }

}
