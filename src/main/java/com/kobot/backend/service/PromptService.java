package com.kobot.backend.service;

import com.kobot.backend.CacheDto;
import com.kobot.backend.query.cache.QueryCache;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final QueryCache queryCache;

    public String getChatResponse(String query) {
        CacheDto cached = queryCache.getCached(query);
        if (cached != null) {
            return cached.getAnswer();
        }

        final OpenAiChatOptions options = OpenAiChatOptions.builder()
            .withModel("gpt-4")
            .withTemperature(0.2F).build();

        String answer = ChatClient.builder(chatModel)
            .build().prompt()
            .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
            .user(query).options(options)
            .call()
            .chatResponse().getResult().getOutput().getContent();

        queryCache.cache(query, answer);

        return answer;
    }
}
