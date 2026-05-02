package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.rag.QueryRewriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SpringAiApp {

    private final ChatClient chatClient;
    private final VectorStore springAiVectorStore;
    private final QueryRewriter queryRewriter;

    private static final String SYSTEM_PROMPT = """
            你是 Spring AI 专家助手，专门回答关于 Spring AI 框架的问题。

            请遵循以下原则：
            1. 基于提供的知识库文档回答问题
            2. 如果知识库中没有相关信息，明确告知用户
            3. 回答要准确、专业、简洁
            4. 适当使用代码示例说明
            5. 如果问题涉及多个方面，分点说明

            知识库来源：Spring AI 官方文档
            """;

    public SpringAiApp(ChatModel dashscopeChatModel,
                       VectorStore springAiVectorStore,
                       QueryRewriter queryRewriter) {
        this.springAiVectorStore = springAiVectorStore;
        this.queryRewriter = queryRewriter;

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();

        log.info("SpringAiApp 初始化完成");
    }

    public List<RetrievedDocument> retrieveDocuments(String query, int topK) {
        log.info("检索文档: query={}, topK={}", query, topK);

        String rewrittenQuery = queryRewriter.doQueryRewrite(query);
        log.info("查询重写: {} -> {}", query, rewrittenQuery);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(rewrittenQuery)
                .topK(topK)
                .similarityThreshold(0.3)
                .build();

        List<Document> documents = springAiVectorStore.similaritySearch(searchRequest);

        List<RetrievedDocument> result = documents.stream()
                .map(this::convertToRetrievedDocument)
                .collect(Collectors.toList());

        log.info("检索到 {} 个相关文档", result.size());
        return result;
    }

    public List<RetrievedDocument> retrieveDocuments(String query) {
        return retrieveDocuments(query, 5);
    }

    private RetrievedDocument convertToRetrievedDocument(Document document) {
        RetrievedDocument dto = new RetrievedDocument();

        String content = document.getText();
        dto.setContent(content.length() > 1000 ? content.substring(0, 1000) + "..." : content);

        if (document.getMetadata() != null) {
            dto.setTitle((String) document.getMetadata().getOrDefault("title", "未知标题"));
            dto.setSource((String) document.getMetadata().getOrDefault("source", "spring-ai"));
        } else {
            dto.setTitle("未知标题");
            dto.setSource("spring-ai");
        }

        Double score = document.getScore();
        dto.setScore(score != null ? score : 0.0);

        return dto;
    }

    public String doChat(String message, String chatId) {
        log.info("同步对话: chatId={}, message={}", chatId, message);

        // 创建查询重写转换器
        RewriteQueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .build();

        // 创建文档检索器
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(springAiVectorStore)
                .similarityThreshold(0.3)
                .topK(5)
                .build();

        // 创建查询增强器，允许空上下文时使用模型自身知识
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        // 创建 RAG Advisor，包含查询重写
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(ragAdvisor)
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("AI 回答: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return doChatByStream(message, chatId, null);
    }

    public Flux<String> doChatByStream(String message, String chatId, List<RetrievedDocument> preRetrievedDocuments) {
        log.info("流式对话: chatId={}, message={}, preRetrievedDocuments={}", chatId, message, 
                preRetrievedDocuments != null ? preRetrievedDocuments.size() : "null");

        if (preRetrievedDocuments != null && !preRetrievedDocuments.isEmpty()) {
            return doChatByStreamWithPreRetrievedDocs(message, chatId, preRetrievedDocuments);
        }

        return doChatByStreamWithRAG(message, chatId);
    }

    private Flux<String> doChatByStreamWithRAG(String message, String chatId) {
        RewriteQueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .build();

        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(springAiVectorStore)
                .similarityThreshold(0.3)
                .topK(5)
                .build();

        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();

        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(ragAdvisor)
                .stream()
                .content();
    }

    private Flux<String> doChatByStreamWithPreRetrievedDocs(String message, String chatId, List<RetrievedDocument> preRetrievedDocuments) {
        log.info("使用预检索文档生成回答: chatId={}, 文档数={}", chatId, preRetrievedDocuments.size());

        String context = preRetrievedDocuments.stream()
                .map(doc -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("标题: ").append(doc.getTitle() != null ? doc.getTitle() : "未知").append("\n");
                    sb.append("来源: ").append(doc.getSource() != null ? doc.getSource() : "未知").append("\n");
                    sb.append("相关度: ").append(String.format("%.2f", doc.getScore() * 100)).append("%\n");
                    sb.append("内容: ").append(doc.getContent() != null ? doc.getContent() : "");
                    return sb.toString();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String enhancedMessage = String.format(
                "请基于以下知识片段回答问题。如果提供的知识片段中没有相关信息，请明确告知用户。\n\n=== 知识片段 ===\n%s\n\n=== 问题 ===\n%s",
                context, message
        );

        return chatClient
                .prompt()
                .user(enhancedMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    public static class RetrievedDocument {
        private String title;
        private String content;
        private String source;
        private double score;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        @Override
        public String toString() {
            return "RetrievedDocument{" +
                    "title='" + title + '\'' +
                    ", source='" + source + '\'' +
                    ", score=" + score +
                    ", contentLength=" + (content != null ? content.length() : 0) +
                    '}';
        }
    }
}