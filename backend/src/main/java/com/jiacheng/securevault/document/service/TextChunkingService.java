package com.jiacheng.securevault.document.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class TextChunkingService {

    private static final String STRONG_BOUNDARIES = "。！？；.?!;";
    private static final String WEAK_BOUNDARIES = "，,、 ";

    private final ChunkingProperties properties;

    public TextChunkingService(ChunkingProperties properties) {
        this.properties = properties;
    }

    public List<ChunkCandidate> split(String text) {
        String cleaned = clean(text);
        if (cleaned.isBlank()) {
            return List.of();
        }
        if (cleaned.length() <= properties.getChunkSize()) {
            return List.of(toCandidate(0, cleaned, 0, cleaned.length()));
        }

        List<Range> ranges = splitRanges(cleaned);
        List<ChunkCandidate> chunks = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            String content = cleaned.substring(range.startOffset(), range.endOffset());
            chunks.add(toCandidate(i, content, range.startOffset(), range.endOffset()));
        }
        return chunks;
    }

    public String clean(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u0000", "");
        normalized = trimLineRight(normalized);
        normalized = compressBlankLines(normalized);
        return normalized.trim();
    }

    private List<Range> splitRanges(String text) {
        int length = text.length();
        if (length <= properties.getChunkSize()) {
            return List.of(trimRange(text, 0, length));
        }

        List<Range> ranges = new ArrayList<>();
        int start = 0;
        while (start < length) {
            int rawEnd = Math.min(start + properties.getChunkSize(), length);
            int end = rawEnd < length ? findBoundary(text, start, rawEnd) : rawEnd;
            if (end <= start) {
                end = rawEnd;
            }
            if (end <= start) {
                end = Math.min(start + 1, length);
            }

            Range range = trimRange(text, start, end);
            if (!range.isEmpty()) {
                ranges.add(range);
            }

            if (end >= length) {
                break;
            }

            int nextStart = end - properties.getOverlapSize();
            if (nextStart <= start) {
                nextStart = end;
            }
            if (nextStart <= start) {
                nextStart = start + 1;
            }
            start = Math.min(nextStart, length);
        }

        return mergeSmallTail(text, ranges);
    }

    private int findBoundary(String text, int start, int rawEnd) {
        int minBoundary = start + Math.max(1, (int) ((rawEnd - start) * 0.6));

        int paragraph = text.lastIndexOf("\n\n", rawEnd - 1);
        if (paragraph >= minBoundary && paragraph < rawEnd) {
            return paragraph + 2;
        }

        int newline = text.lastIndexOf('\n', rawEnd - 1);
        if (newline >= minBoundary && newline < rawEnd) {
            return newline + 1;
        }

        int strong = findLastBoundaryChar(text, minBoundary, rawEnd, STRONG_BOUNDARIES);
        if (strong > 0) {
            return strong;
        }

        int weak = findLastBoundaryChar(text, minBoundary, rawEnd, WEAK_BOUNDARIES);
        if (weak > 0) {
            return weak;
        }

        return rawEnd;
    }

    private int findLastBoundaryChar(String text, int minBoundary, int rawEnd, String boundaries) {
        for (int i = rawEnd - 1; i >= minBoundary; i--) {
            if (boundaries.indexOf(text.charAt(i)) >= 0) {
                return i + 1;
            }
        }
        return -1;
    }

    private String trimLineRight(String text) {
        StringBuilder result = new StringBuilder(text.length());
        StringBuilder pendingWhitespace = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\f') {
                pendingWhitespace.append(ch);
            } else if (ch == '\n') {
                pendingWhitespace.setLength(0);
                result.append(ch);
            } else {
                if (pendingWhitespace.length() > 0) {
                    result.append(pendingWhitespace);
                    pendingWhitespace.setLength(0);
                }
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String compressBlankLines(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int consecutiveNewlines = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                consecutiveNewlines++;
                if (consecutiveNewlines <= 2) {
                    result.append(ch);
                }
            } else {
                consecutiveNewlines = 0;
                result.append(ch);
            }
        }
        return result.toString();
    }

    private ChunkCandidate toCandidate(int index, String content, int startOffset, int endOffset) {
        return new ChunkCandidate(
                index,
                content,
                content.length(),
                estimateTokenCount(content),
                sha256(content),
                startOffset,
                endOffset
        );
    }

    private List<Range> mergeSmallTail(String text, List<Range> ranges) {
        if (ranges.size() <= 1) {
            return ranges;
        }
        Range last = ranges.get(ranges.size() - 1);
        if (last.length() >= properties.getMinChunkSize()) {
            return ranges;
        }

        Range previous = ranges.get(ranges.size() - 2);
        Range merged = trimRange(text, previous.startOffset(), last.endOffset());
        List<Range> mergedRanges = new ArrayList<>(ranges.subList(0, ranges.size() - 2));
        mergedRanges.add(merged);
        return mergedRanges;
    }

    private Range trimRange(String text, int start, int end) {
        int trimmedStart = start;
        int trimmedEnd = end;
        while (trimmedStart < trimmedEnd && Character.isWhitespace(text.charAt(trimmedStart))) {
            trimmedStart++;
        }
        while (trimmedEnd > trimmedStart && Character.isWhitespace(text.charAt(trimmedEnd - 1))) {
            trimmedEnd--;
        }
        return new Range(trimmedStart, trimmedEnd);
    }

    // This is a rough estimate for sizing only. It is not a model tokenizer.
    private int estimateTokenCount(String content) {
        int count = 0;
        boolean inAsciiWord = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (isCjk(ch)) {
                count++;
                inAsciiWord = false;
            } else if (Character.isLetterOrDigit(ch)) {
                if (!inAsciiWord) {
                    count++;
                    inAsciiWord = true;
                }
            } else {
                inAsciiWord = false;
                if (!Character.isWhitespace(ch)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private record Range(int startOffset, int endOffset) {
        boolean isEmpty() {
            return startOffset >= endOffset;
        }

        int length() {
            return endOffset - startOffset;
        }
    }

    public record ChunkCandidate(int chunkIndex,
                                 String content,
                                 int contentLength,
                                 int tokenCount,
                                 String contentHash,
                                 int startOffset,
                                 int endOffset) {
    }
}
