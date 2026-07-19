package com.explodingkittens.backend.modules.game.explodingkittens.model;

import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.GamePhase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameState implements com.explodingkittens.backend.modules.game.core.GameState {
    private final String roomId;
    private final List<Player> players = new ArrayList<>();
    private List<Card> drawPile = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();
    private List<Card> lastPlayedCards = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private GamePhase phase = GamePhase.WAITING_FOR_PLAYERS;
    private PendingAction pendingAction;
    private String winnerPlayerId;
    private Instant lastActivityAt = Instant.now();

    public GameState(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Card> getDrawPile() {
        return drawPile;
    }

    public void setDrawPile(List<Card> drawPile) {
        this.drawPile = drawPile == null ? new ArrayList<>() : drawPile;
    }

    public List<Card> getDiscardPile() {
        return discardPile;
    }

    public void setDiscardPile(List<Card> discardPile) {
        this.discardPile = discardPile == null ? new ArrayList<>() : discardPile;
    }

    public List<Card> getLastPlayedCards() {
        return lastPlayedCards;
    }

    public void setLastPlayedCards(List<Card> lastPlayedCards) {
        this.lastPlayedCards = lastPlayedCards == null ? new ArrayList<>() : lastPlayedCards;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public PendingAction getPendingAction() {
        return pendingAction;
    }

    public void setPendingAction(PendingAction pendingAction) {
        this.pendingAction = pendingAction;
    }

    public String getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(String winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public Optional<Player> getCurrentPlayer() {
        if (players.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return Optional.empty();
        }
        return Optional.of(players.get(currentPlayerIndex));
    }

    @Override
    public boolean isFinished() {
        return phase == GamePhase.FINISHED;
    }
}
