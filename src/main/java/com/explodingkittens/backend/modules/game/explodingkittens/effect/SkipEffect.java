package com.explodingkittens.backend.modules.game.explodingkittens.effect;

import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import org.springframework.stereotype.Component;

@Component
public class SkipEffect implements CardEffect {
    @Override
    public CardType supports() {
        return CardType.SKIP;
    }

    @Override
    public void apply(GameState state, EffectContext context, GameResult result) {
        context.turnManager().finishTurnWithoutDraw(state);
        result.setPublicMessage(context.actor().getName() + " đã bỏ qua lượt rút bài.");
    }
}
