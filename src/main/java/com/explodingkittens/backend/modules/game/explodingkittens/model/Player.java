package com.explodingkittens.backend.modules.game.explodingkittens.model;

import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.PlayerStatus;

import java.util.ArrayList;
import java.util.List;

public class Player implements com.explodingkittens.backend.modules.game.core.Player {
    private final String id;
    private String name;
    private int seatIndex;
    private List<Card> hand = new ArrayList<>();
    private PlayerStatus status = PlayerStatus.CONNECTED;
    private int pendingTurns = 0;
    private final boolean isBot;

    public Player(String id, String name, int seatIndex) {
        this(id, name, seatIndex, false);
    }

    public Player(String id, String name, int seatIndex, boolean isBot) {
        this.id = id;
        this.name = name;
        this.seatIndex = seatIndex;
        this.isBot = isBot;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand == null ? new ArrayList<>() : hand;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public int getPendingTurns() {
        return pendingTurns;
    }

    public void setPendingTurns(int pendingTurns) {
        this.pendingTurns = pendingTurns;
    }

    public boolean isBot() {
        return isBot;
    }

    @Override
    public boolean isAlive() {
        return status != PlayerStatus.DEAD;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void removeCard(Card card) {
        hand.remove(card);
    }
}
