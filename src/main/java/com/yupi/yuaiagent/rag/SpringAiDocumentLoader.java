package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SpringAiDocumentLoader {

    private final ResourceLoader resourceLoader;

    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^#+\\s+(.*)");

    public SpringAiDocumentLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<Document> loadDocuments() {
        List<Document> documents = new ArrayList<>();

        try {
            Resource resource = resourceLoader.getResource("classpath:document/spring-ai/spring-ai");

            if (!resource.exists()) {
                log.error("Spring AI 文档不存在: classpath:document/spring-ai/spring-ai");
                return documents;
            }

            String content = readResourceContent(resource);
            documents = splitByHeadings(content);

            log.info("成功加载 Spring AI 文档，共 {} 个片段", documents.size());

        } catch (IOException e) {
            log.error("加载 Spring AI 文档失败", e);
        }

        return documents;
    }

    private String readResourceContent(Resource resource) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    private List<Document> splitByHeadings(String content) {
        List<Document> documents = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(content);

        int lastEnd = 0;
        String lastTitle = "Introduction";
        int sectionIndex = 0;

        while (matcher.find()) {
            if (lastEnd > 0) {
                String sectionContent = content.substring(lastEnd, matcher.start()).trim();

                if (!sectionContent.isEmpty()) {
                    Document doc = createDocument(sectionContent, lastTitle, sectionIndex);
                    documents.add(doc);
                    sectionIndex++;
                }
            }

            lastTitle = matcher.group(1).trim();
            lastEnd = matcher.end();
        }

        if (lastEnd < content.length()) {
            String sectionContent = content.substring(lastEnd).trim();
            if (!sectionContent.isEmpty()) {
                Document doc = createDocument(sectionContent, lastTitle, sectionIndex);
                documents.add(doc);
            }
        }

        return documents;
    }

    private Document createDocument(String content, String title, int index) {
        String cleanedContent = content.replace("\0", "");
        Map<String, Object> metadata = Map.of(
                "title", title,
                "source", "spring-ai",
                "sectionIndex", index,
                "type", "documentation"
        );

        return new Document(cleanedContent, metadata);
    }

    public List<Document> reloadDocuments() {
        log.info("重新加载 Spring AI 文档...");
        return loadDocuments();
    }
}