package com.explodingkittens.backend.modules.game.explodingkittens.effect;

import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Player;
import com.explodingkittens.backend.modules.game.explodingkittens.service.TurnManager;

import java.util.List;
import java.util.Random;

public class EffectContext {
    private final Player actor;
    private final Player target;
    private final List<Card> cards;
    private final Random random;
    private final TurnManager turnManager;

    public EffectContext(Player actor, Player target, List<Card> cards, Random random, TurnManager turnManager) {
        this.actor = actor;
        this.target = target;
        this.cards = cards;
        this.random = random;
        this.turnManager = turnManager;
    }

    public Player actor() {
        return actor;
    }

    public Player target() {
        return target;
    }

    public List<Card> cards() {
        return cards;
    }

    public Random random() {
        return random;
    }

    public TurnManager turnManager() {
        return turnManager;
    }
}
