package com.kobot.backend.query.cache;

import com.kobot.backend.CacheDto;

public interface QueryCache {

    /**
     * 질의와 답변을 캐시합니다.
     *
     * @param query  질의
     * @param answer 답변
     * @return 캐시된 객체. 캐시 실패 시, null
     */
    CacheDto cache(String query, String answer);

    /**
     * 질의와 유사한 질답을 캐시에서 꺼냅니다.
     *
     * @param query 질의
     * @return 질답. cache miss 시, null
     */
    CacheDto getCached(String query);

    /**
     * 일정 시간이 된 캐시는 삭제합니다.
     *
     * @param from 삭제할 캐시 생성 시각의 최솟값
     * @param to   삭제할 캐시 최대 생성 시각 최댓값
     */
    void clear(long from, long to);

}
