package com.kobot.backend.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.IntArrayList;
import com.knuddels.jtokkit.api.ModelType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

  private final VectorStore vectorStore;
  private final ElasticsearchClient client;
  private final String indexName = "spring-ai-document-index";

  public void saveEmbedding(String text) {
    createIndexIfNotExists();
    List<Document> documents = List.of(
        new Document(text)
    );
    vectorStore.add(documents);
  }

  public void savePdfEmbedding(MultipartFile file) {
    createIndexIfNotExists();
    try {
      TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(
          new InputStreamResource(file.getInputStream()));
      List<Document> documents = tikaDocumentReader.read();

      List<Document> splitDocuments = new ArrayList<>();
      EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
      Encoding enc = registry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
      final int maxTokens = 1000;

      for (Document doc : documents) {
        String content = doc.getContent();
        IntArrayList tokens = enc.encode(content);

        for (int i = 0; i < tokens.size(); i += maxTokens) {
          int end = Math.min(i + maxTokens, tokens.size());
          IntArrayList chunkTokens = new IntArrayList();
          for (int j = i; j < end; j++) {
            chunkTokens.add(tokens.get(j));
          }
          String chunkText = enc.decode(chunkTokens);

          Document splitDoc = new Document(chunkText, new HashMap<>(doc.getMetadata()));
          splitDoc.getMetadata().put("chunk_id", i / maxTokens);
          splitDocuments.add(splitDoc);
        }

      }

      addDocuments(splitDocuments);
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  private void addDocuments(List<Document> documents) throws IOException {
    vectorStore.add(documents);
    List<BulkOperation> operations = documents.stream()
        .map(doc -> new BulkOperation.Builder()
            .index(new IndexOperation.Builder<Document>()
                .index("spring-ai-document-index")
                .document(doc)
                .build())
            .build())
        .toList();

    BulkRequest request = new BulkRequest.Builder()
        .operations(operations)
        .build();

    client.bulk(request);
  }

  private void createIndexIfNotExists() {
    try {
      boolean indexExists = client.indices().exists(b -> b.index(indexName)).value();
      if (!indexExists) {
        client.indices().create(c -> c.index(indexName));
        log.info("Index {} created.", indexName);
      } else {
        log.info("Index {} already exists.", indexName);
      }
    } catch (IOException e) {
      log.error("Error checking or creating index: {}", e.getMessage(), e);
    }
  }
}
