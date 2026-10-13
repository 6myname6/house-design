package com.housedesign.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PromptSanitizerTest {

    @Test
    void nullReturnsEmpty() {
        assertTrue(PromptSanitizer.sanitize(null, 100).isEmpty());
    }

    @Test
    void removesControlCharsButKeepsNewline() {
        // BEL(7) 与 DEL(127) 被删除；换行保留；制表符按空白收敛规则折成空格
        String raw = "a" + '\t' + " b" + '\n' + "c" + (char) 7 + "d" + (char) 127 + "e";
        assertEquals("a b" + '\n' + "cde", PromptSanitizer.sanitize(raw, 100));
    }

    @Test
    void collapsesRepeatedSpaces() {
        assertEquals("a b c", PromptSanitizer.sanitize("a   b   c", 100));
    }

    @Test
    void truncatesToMaxLen() {
        assertEquals("abc", PromptSanitizer.sanitize("abcdef", 3));
    }

    @Test
    void trimsEnds() {
        assertEquals("你好", PromptSanitizer.sanitize("  你好  ", 100));
    }
}
