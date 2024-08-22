package com.kobot.backend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@Builder
@ToString
public class CacheDto {

    private String id;
    private String query;
    private String answer;
    private float distance;
}
