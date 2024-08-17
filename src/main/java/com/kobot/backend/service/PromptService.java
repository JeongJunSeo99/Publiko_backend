package com.kobot.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptService {

  private final ChatModel chatModel;
  private final VectorStore vectorStore;

  public ChatResponse getChatResponse(String query) {

    final OpenAiChatOptions options = OpenAiChatOptions.builder()
        .withModel("gpt-4")
        .withTemperature(0.2F).build();

    return ChatClient.builder(chatModel)
        .build().prompt()
        .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
        .user(query).options(options)
        .call()
        .chatResponse();
  }
}
