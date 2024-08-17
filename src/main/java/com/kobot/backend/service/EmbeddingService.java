package com.kobot.backend.service;

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

  public void saveEmbedding(String text) {
    List<Document> documents = List.of(
        new Document(text)
    );
    vectorStore.add(documents);
  }

  public void savePdfEmbedding(MultipartFile file) {
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

        // 문장 단위로 분할
        String[] sentences = content.split("(?<=[.?!])\\s*");
        IntArrayList chunkTokens = new IntArrayList();

        for (String sentence : sentences) {
          IntArrayList sentenceTokens = enc.encode(sentence);

          // 기존 청크에 추가할 경우 maxTokens를 초과하는지 확인
          if (chunkTokens.size() + sentenceTokens.size() > maxTokens) {
            // 새로운 chunk 생성
            String chunkText = enc.decode(chunkTokens);
            Document splitDoc = new Document(chunkText, new HashMap<>(doc.getMetadata()));
            splitDocuments.add(splitDoc);

            // 새로운 chunk 시작
            chunkTokens = new IntArrayList();
          }

          // 수동으로 tokens를 추가
          for (int j = 0; j < sentenceTokens.size(); j++) {
            chunkTokens.add(sentenceTokens.get(j));
          }
        }

        // 마지막 chunk 처리
        if (!chunkTokens.isEmpty()) {
          String chunkText = enc.decode(chunkTokens);
          Document splitDoc = new Document(chunkText, new HashMap<>(doc.getMetadata()));
          splitDocuments.add(splitDoc);
        }
      }

      vectorStore.add(splitDocuments);
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

}
