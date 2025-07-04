package com.example.ourmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
    @Test
    public void validateItemNameRejectsInvalid() {
        assertFalse(Config.validateItemName(123));
    }
}
