package com.explodingkittens.backend.modules.game.explodingkittens.model;

import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.PendingActionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PendingAction {
    private final String id;
    private final PendingActionType type;
    private final String actorId;
    private final List<Card> playedCards;
    private String targetPlayerId;
    private int nopeCount;
    private Instant expiresAt;

    public PendingAction(PendingActionType type, String actorId, List<Card> playedCards) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.actorId = actorId;
        this.playedCards = playedCards;
        this.nopeCount = 0;
    }

    public String getId() {
        return id;
    }

    public PendingActionType getType() {
        return type;
    }

    public String getActorId() {
        return actorId;
    }

    public List<Card> getPlayedCards() {
        return playedCards;
    }

    public String getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(String targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public int getNopeCount() {
        return nopeCount;
    }

    public void setNopeCount(int nopeCount) {
        this.nopeCount = nopeCount;
    }

    public boolean isNoped() {
        return nopeCount % 2 != 0;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
