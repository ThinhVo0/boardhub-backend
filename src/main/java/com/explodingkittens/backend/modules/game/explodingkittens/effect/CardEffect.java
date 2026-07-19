package com.explodingkittens.backend.modules.game.explodingkittens.effect;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.BotPlay;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Player;

import java.util.List;
import java.util.Optional;

public interface CardEffect {
    CardType supports();

    void apply(GameState state, EffectContext context, GameResult result);

    default boolean requiresTarget() {
        return false;
    }

    default Optional<BotPlay> evaluateBotPlay(Player bot, GameState state, Card card, List<Player> targets) {
        return Optional.of(new BotPlay(List.of(card.getId()), null));
    }
}
