package com.explodingkittens.backend.modules.game.explodingkittens.effect;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;

import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ShuffleEffect implements CardEffect {
    @Override
    public CardType supports() {
        return CardType.SHUFFLE;
    }

    @Override
    public void apply(GameState state, EffectContext context, GameResult result) {
        Collections.shuffle(state.getDrawPile(), context.random());
        result.setPublicMessage(context.actor().getName() + " đã xáo bài.");
    }
}
