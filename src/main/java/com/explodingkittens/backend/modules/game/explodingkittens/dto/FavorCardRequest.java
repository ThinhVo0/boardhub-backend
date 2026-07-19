package com.explodingkittens.backend.modules.game.explodingkittens.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavorCardRequest {
    private String roomId;
    private String playerId;
    private String cardId;
}