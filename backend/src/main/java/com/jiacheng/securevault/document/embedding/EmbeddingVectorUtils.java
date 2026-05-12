package com.jiacheng.securevault.document.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmbeddingVectorUtils {

    private EmbeddingVectorUtils() {
    }

    public static void validate(List<Double> vector, int expectedDimension) {
        if (vector == null) {
            throw new EmbeddingException("Embedding vector is empty");
        }
        if (vector.size() != expectedDimension) {
            throw new EmbeddingException("Embedding dimension mismatch");
        }
        for (Double value : vector) {
            if (value == null || value.isNaN() || value.isInfinite()) {
                throw new EmbeddingException("Embedding vector contains invalid number");
            }
        }
    }

    public static String toPgvectorString(List<Double> vector, int expectedDimension) {
        validate(vector, expectedDimension);
        StringBuilder builder = new StringBuilder(vector.size() * 12);
        builder.append('[');
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(format(vector.get(i)));
        }
        builder.append(']');
        return builder.toString();
    }

    public static String toJson(List<Double> vector, int expectedDimension) {
        return toPgvectorString(vector, expectedDimension);
    }

    public static List<Double> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new EmbeddingException("Embedding vector format is invalid");
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        String[] parts = body.split(",");
        List<Double> vector = new ArrayList<>(parts.length);
        for (String part : parts) {
            vector.add(Double.parseDouble(part.trim()));
        }
        return vector;
    }

    public static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return 0.0d;
        }
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < left.size(); i++) {
            double l = left.get(i);
            double r = right.get(i);
            if (!Double.isFinite(l) || !Double.isFinite(r)) {
                return 0.0d;
            }
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        double score = dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
        if (!Double.isFinite(score)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    public static List<Double> normalize(List<Double> vector) {
        double norm = 0.0d;
        for (double value : vector) {
            norm += value * value;
        }
        if (norm == 0.0d) {
            return vector;
        }
        double sqrt = Math.sqrt(norm);
        return vector.stream().map(value -> value / sqrt).toList();
    }

    public static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new EmbeddingException("Embedding hash failed", ex);
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.10f", value);
    }
}
