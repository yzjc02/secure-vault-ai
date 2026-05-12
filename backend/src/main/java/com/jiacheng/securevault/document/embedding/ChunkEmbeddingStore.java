package com.jiacheng.securevault.document.embedding;

import com.jiacheng.securevault.document.dto.SimilarChunkResponse;

import java.util.List;

public interface ChunkEmbeddingStore {

    void saveEmbedding(Long userId, Long chunkId, List<Double> embedding, String model, int dimension);

    int countEmbeddedChunks(Long userId, Long documentId);

    List<SimilarChunkResponse> search(Long userId,
                                      String queryVectorString,
                                      List<Double> queryEmbedding,
                                      Long documentId,
                                      int topK);
}
