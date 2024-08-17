package com.kobot.backend.controller;

import com.kobot.backend.service.EmbeddingService;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

  private final EmbeddingService embeddingService;

  @PostMapping("/text")
  public void EmbeddingText(@RequestBody String text) {
    embeddingService.saveEmbedding(text);
  }

  @PostMapping("/pdf")
  public void EmbeddingPdf(@RequestPart("file") @Nullable MultipartFile file){
    embeddingService.savePdfEmbedding(Objects.requireNonNull(file));
  }
}
