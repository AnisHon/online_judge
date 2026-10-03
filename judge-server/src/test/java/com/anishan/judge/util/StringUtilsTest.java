package com.anishan.judge.util;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

    @Test
    void contentEqualsIgnoresBlankCharactersAndDifferentLineEndings() {
        StringReader expected = new StringReader("123    \n123123                  \n");
        StringReader actual = new StringReader("123 \r\n123123 ");

        assertTrue(StringUtils.contentEqualsIgnoreBlankAndEOL(expected, actual));
    }
}
