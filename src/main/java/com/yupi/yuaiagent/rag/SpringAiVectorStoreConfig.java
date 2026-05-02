package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
@Slf4j
public class SpringAiVectorStoreConfig {

    @Resource
    private SpringAiDocumentLoader springAiDocumentLoader;

    @Bean
    public VectorStore springAiVectorStore(JdbcTemplate jdbcTemplate,
                                            EmbeddingModel dashscopeEmbeddingModel) {
        log.info("初始化 Spring AI 向量存储...");

        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("spring_ai_vector_store")
                .maxDocumentBatchSize(10000)
                .build();

        loadDocumentsToVectorStore(vectorStore, jdbcTemplate);

        log.info("Spring AI 向量存储初始化完成");
        return vectorStore;
    }

    private void loadDocumentsToVectorStore(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        try {
            if (isVectorStoreEmpty(jdbcTemplate)) {
                log.info("向量存储为空，开始加载文档...");

                List<Document> documents = springAiDocumentLoader.loadDocuments();

                if (documents.isEmpty()) {
                    log.warn("未加载到任何文档，请检查文档路径");
                    return;
                }

                log.info("加载到 {} 个文档片段，开始向量化...", documents.size());
                batchAddDocuments(vectorStore, documents);
                log.info("成功将 {} 个文档片段写入向量存储", documents.size());
            } else {
                log.info("向量存储中已有数据，跳过文档加载");
            }
        } catch (Exception e) {
            log.error("加载文档到向量存储失败", e);
        }
    }

    private void batchAddDocuments(VectorStore vectorStore, List<Document> documents) {
        int batchSize = 20;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            log.info("处理批次 {}-{} (共 {} 个)", i + 1, end, batch.size());
            vectorStore.add(batch);
        }
    }

    private boolean isVectorStoreEmpty(JdbcTemplate jdbcTemplate) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM spring_ai_vector_store", Long.class);
            boolean isEmpty = count == null || count == 0;
            log.debug("向量存储记录数: {}, 是否为空: {}", count, isEmpty);
            return isEmpty;
        } catch (Exception e) {
            log.warn("检查向量存储状态时出错，假设为空: {}", e.getMessage());
            return true;
        }
    }

    public void refreshVectorStore(VectorStore vectorStore) {
        log.info("刷新 Spring AI 向量存储...");

        try {
            List<Document> documents = springAiDocumentLoader.reloadDocuments();
            batchAddDocuments(vectorStore, documents);
            log.info("向量存储刷新完成，共 {} 个文档", documents.size());
        } catch (Exception e) {
            log.error("刷新向量存储失败", e);
            throw new RuntimeException("刷新向量存储失败", e);
        }
    }
}