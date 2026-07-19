package com.explodingkittens.backend.modules.game.explodingkittens.model.enums;

public enum CardType {
    EXPLODING_KITTEN(CardCategory.HAZARD, "Mèo Nổ", 4),
    DEFUSE(CardCategory.REACTION, "Gỡ Bom", 6),
    SKIP(CardCategory.ACTION, "Qua Lượt", 4),
    ATTACK(CardCategory.ACTION, "Tấn Công", 4),
    FAVOR(CardCategory.ACTION, "Xin Bài", 4),
    SHUFFLE(CardCategory.ACTION, "Xáo Bài", 4),
    SEE_THE_FUTURE(CardCategory.ACTION, "Xem Tương Lai", 5),
    NOPE(CardCategory.REACTION, "Phủ Nhận (Nope)", 5),
    REVERSE(CardCategory.ACTION, "Đảo Lượt", 0),
    TACOCAT(CardCategory.CAT, "Mèo Tacocat", 5),
    BEARD_CAT(CardCategory.CAT, "Mèo Râu", 5),
    CATTERMELON(CardCategory.CAT, "Mèo Dưa Hấu", 5),
    HAIRY_POTATO_CAT(CardCategory.CAT, "Mèo Khoai Tây", 5);

    private final CardCategory category;
    private final String displayName;
    private final int defaultDeckCount;

    CardType(CardCategory category, String displayName, int defaultDeckCount) {
        this.category = category;
        this.displayName = displayName;
        this.defaultDeckCount = defaultDeckCount;
    }

    public CardCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultDeckCount() {
        return defaultDeckCount;
    }

    public boolean isCatCard() {
        return category == CardCategory.CAT;
    }
}
