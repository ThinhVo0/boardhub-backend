package com.explodingkittens.backend.modules.game.explodingkittens.model;

import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;

import java.util.UUID;

public class Card {
    private final String id;
    private final CardType type;

    public Card(CardType type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public CardType getType() {
        return type;
    }

    public String getDisplayName() {
        return type.getDisplayName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return id.equals(card.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
