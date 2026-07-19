package com.explodingkittens.backend.modules.game.explodingkittens.effect;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.dto.PrivateGameEvent;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeeTheFutureEffect implements CardEffect {
    @Override
    public CardType supports() {
        return CardType.SEE_THE_FUTURE;
    }

    @Override
    public void apply(GameState state, EffectContext context, GameResult result) {
        List<Card> topCards = state.getDrawPile().stream().limit(3).toList();
        result.addPrivateEvent(new PrivateGameEvent(context.actor().getId(), "SEE_THE_FUTURE", topCards));
        result.setPublicMessage(context.actor().getName() + " đã nhìn trước tương lai.");
    }
}
