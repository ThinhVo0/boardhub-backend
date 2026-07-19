package com.explodingkittens.backend.modules.game.explodingkittens.effect;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.BotPlay;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.GamePhase;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.PendingActionType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.PendingAction;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
public class FavorEffect implements CardEffect {
    @Override
    public CardType supports() {
        return CardType.FAVOR;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public Optional<BotPlay> evaluateBotPlay(Player bot, GameState state, Card card, List<Player> targets) {
        List<Player> validTargets = targets.stream()
                .filter(t -> !t.getHand().isEmpty())
                .toList();
        if (validTargets.isEmpty()) {
            return Optional.empty();
        }
        Player target = validTargets.get(new Random().nextInt(validTargets.size()));
        return Optional.of(new BotPlay(List.of(card.getId()), target.getId()));
    }

    @Override
    public void apply(GameState state, EffectContext context, GameResult result) {
        if (context.target() == null || !context.target().isAlive()) {
            throw new IllegalArgumentException("Favor requires a live target player.");
        }
        if (context.target().getHand().isEmpty()) {
            result.setPublicMessage(context.actor().getName() + " yêu cầu cho bài, nhưng " + context.target().getName() + " không có lá bài nào.");
            return;
        }

        PendingAction favor = new PendingAction(PendingActionType.FAVOR_CARD_SELECTION, context.actor().getId(), context.cards());
        favor.setTargetPlayerId(context.target().getId());
        state.setPendingAction(favor);
        state.setPhase(GamePhase.WAITING_FOR_FAVOR_CARD);
        result.setPublicMessage(context.actor().getName() + " yêu cầu " + context.target().getName() + " cho 1 lá bài.");
    }
}
