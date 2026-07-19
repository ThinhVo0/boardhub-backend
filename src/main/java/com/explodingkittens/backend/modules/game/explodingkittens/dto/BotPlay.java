package com.explodingkittens.backend.modules.game.explodingkittens.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BotPlay {
    private List<String> cardIds;
    private String targetPlayerId;


}