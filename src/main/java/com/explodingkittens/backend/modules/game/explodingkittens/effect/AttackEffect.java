package com.explodingkittens.backend.modules.game.explodingkittens.effect;


import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;
import org.springframework.stereotype.Component;

@Component
public class AttackEffect implements CardEffect {
    @Override
    public CardType supports() {
        return CardType.ATTACK;
    }

    @Override
    public void apply(GameState state, EffectContext context, GameResult result) {
        context.turnManager().addTurnsToNextPlayer(state, 2);
        context.turnManager().finishTurnWithoutDraw(state);
        result.setPublicMessage(context.actor().getName() + " đã đánh Tấn Công. Người chơi tiếp theo phải chơi 2 lượt liên tiếp.");
    }
}
