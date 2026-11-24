package models;

import lombok.Getter;

@Getter
public enum JsonField {
    USERNAME("username");

    private final String name;

    JsonField(String name) {
        this.name = name;
    }

}
