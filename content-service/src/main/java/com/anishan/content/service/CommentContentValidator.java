package com.anishan.content.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.dto.CommentCreateRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/** Normalizes plain comment text without interpreting Markdown, HTML, URLs, or emoji aliases. */
@Component
public class CommentContentValidator {
    public static final int MAX_CODE_POINTS = 2000;
    public static final int MAX_UTF8_BYTES = 8 * 1024;

    public CommentCreateRequest normalize(CommentCreateRequest request) {
        if (request == null) throw new ApiStatusException(400, "评论内容有误");
        return new CommentCreateRequest()
                .setContent(normalizeContent(request.getContent()))
                .setClientRequestId(normalizeRequestId(request.getClientRequestId()));
    }

    public String normalizeContent(String content) {
        if (content == null) throw new ApiStatusException(400, "评论内容不能为空");
        String normalized = Normalizer.normalize(content.replace("\r\n", "\n"), Normalizer.Form.NFC);
        normalized = trimUnicodeWhitespace(normalized);
        validateSurrogatesAndControls(normalized);
        if (normalized.isEmpty()) throw new ApiStatusException(400, "评论内容不能为空");
        if (normalized.codePointCount(0, normalized.length()) > MAX_CODE_POINTS
                || normalized.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            throw new ApiStatusException(400, "评论内容不能超过2000个字符或8KiB");
        }
        return normalized;
    }

    public String normalizeRequestId(String requestId) {
        if (requestId == null) throw new ApiStatusException(400, "请求标识必须是UUID");
        String candidate = requestId.trim();
        try {
            UUID parsed = UUID.fromString(candidate);
            String canonical = parsed.toString();
            if (!canonical.equalsIgnoreCase(candidate)) throw new IllegalArgumentException("non-canonical UUID");
            return canonical.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new ApiStatusException(400, "请求标识必须是UUID");
        }
    }

    private String trimUnicodeWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!isWhitespace(codePoint)) break;
            start += Character.charCount(codePoint);
        }
        while (start < end) {
            int codePoint = value.codePointBefore(end);
            if (!isWhitespace(codePoint)) break;
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    private boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    private void validateSurrogatesAndControls(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw new ApiStatusException(400, "评论内容包含无效字符");
                }
                index++;
                continue;
            }
            if (Character.isLowSurrogate(current) || current == '\0') {
                throw new ApiStatusException(400, "评论内容包含无效字符");
            }
            if (Character.isISOControl(current) && current != '\t' && current != '\n') {
                throw new ApiStatusException(400, "评论内容包含不支持的控制字符");
            }
        }
    }
}
