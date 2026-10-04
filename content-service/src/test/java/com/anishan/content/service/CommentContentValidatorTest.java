package com.anishan.content.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.CommentCreateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommentContentValidatorTest {
    private final CommentContentValidator validator = new CommentContentValidator();

    @Test
    void normalizesLineEndingsUnicodeAndOuterWhitespaceWithoutInterpretingMarkup() {
        String content = " \tCafe\u0301\r\n<script>alert(1)</script> 🙂 \n";
        assertEquals("Café\n<script>alert(1)</script> 🙂",
                validator.normalizeContent(content));
    }

    @Test
    void enforcesCodePointAndUtf8LimitsAtTheExactBoundary() {
        assertEquals(2000, validator.normalizeContent("中".repeat(2000)).codePointCount(0, 2000));
        assertEquals(2000, validator.normalizeContent("🙂".repeat(2000)).codePointCount(0, 4000));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("中".repeat(2001)));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("中".repeat(2731)));
        // At 2000 code points the maximum UTF-8 representation is 8000 bytes, so the
        // independent 8KiB guard is retained but cannot be the tighter boundary.
        assertEquals(8000, validator.normalizeContent("🙂".repeat(2000))
                .getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }

    @Test
    void rejectsBlankNulControlsAndUnpairedSurrogates() {
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent(" \t\n\u00a0"));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("a\u0000b"));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("a\u000bb"));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("a\r b"));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("\ud800"));
        assertThrows(ApiStatusException.class, () -> validator.normalizeContent("\udc00"));
    }

    @Test
    void normalizesCanonicalUuidAndRejectsNonUuid() {
        assertEquals("550e8400-e29b-41d4-a716-446655440000",
                validator.normalizeRequestId("550E8400-E29B-41D4-A716-446655440000"));
        assertThrows(ApiStatusException.class, () -> validator.normalizeRequestId("1-1-1-1-1"));
        assertThrows(ApiStatusException.class, () -> validator.normalize(null));
    }
}
