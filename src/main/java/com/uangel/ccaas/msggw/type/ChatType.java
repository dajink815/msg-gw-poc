package com.uangel.ccaas.msggw.type;

import lombok.Getter;

@Getter
public enum ChatType {
    HANGUP(0), TALK(1), TEL(2), RESERVED(3);

    private final int value;

    ChatType(int value) {
        this.value = value;
    }

    public static ChatType getType(int value) {
        return switch (value) {
            case 0 -> HANGUP;
            case 2 -> TEL;
            case 3 -> RESERVED;
            default -> TALK;
        };
    }
}
