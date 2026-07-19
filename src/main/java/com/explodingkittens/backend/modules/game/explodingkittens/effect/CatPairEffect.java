package com.explodingkittens.backend.modules.game.explodingkittens.effect;


import com.explodingkittens.backend.modules.game.explodingkittens.dto.GameResult;
import com.explodingkittens.backend.modules.game.explodingkittens.model.Card;
import com.explodingkittens.backend.modules.game.explodingkittens.model.GameState;
import com.explodingkittens.backend.modules.game.explodingkittens.model.enums.CardType;

public class CatPairEffect implements CardEffect {
    @Override
    public CardType supports() {
        return null;
    }

    @Override
    public void apply(GameState state, EffectContext context, GameResult result) {
        if (context.target() == null || context.target().getHand().isEmpty()) {
            result.setPublicMessage(context.actor().getName() + " đánh cặp mèo nhưng không trộm được gì.");
            return;
        }

        int index = context.random().nextInt(context.target().getHand().size());
        Card stolen = context.target().getHand().remove(index);
        context.actor().addCard(stolen);
        result.setPublicMessage(context.actor().getName() + " đã ăn trộm 1 lá bài ngẫu nhiên từ " + context.target().getName() + ".");
    }
}
