package com.kobot.backend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CacheDto {

    private String id;
    private String query;
    private String answer;
    private float distance;
}
