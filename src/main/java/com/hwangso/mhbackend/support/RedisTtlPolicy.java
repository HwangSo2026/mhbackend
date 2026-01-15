package com.hwangso.mhbackend.support;

import java.time.*;
/**
 * Redis TTL 정책 유틸 클래스
 * 정책:
 * - 예약 데이터는 "해당 날짜의 자정(다음날 00:00)" 기준으로 만료
 * - Redis EXPIRE 0 방지를 위해 최소 1초 보장
 */
public final class RedisTtlPolicy {

    private RedisTtlPolicy() {}

    /**
     * 특정 date의 자정(다음날 00:00)까지 남은 초 계산
     * Redis EXPIRE 용도
     */

    //plusDay(1)->다음날 atStartOfDay(zoneId)-> 자정
    //ZoneId 받는 이유: 서버 시간대 고정(Asia/Seoul)
    public static long untilMidnight(String date, ZoneId zoneId) {
        LocalDate d = LocalDate.parse(date);
        ZonedDateTime nextMidnight = d.plusDays(1).atStartOfDay(zoneId);

        //초 단위 반환(Redis EXPIRE 요구사항)
        long seconds = Duration
                .between(ZonedDateTime.now(zoneId), nextMidnight)
                .getSeconds();

        // 이미 지난 시간이면 최소 1초 (EXPIRE 0 방지)
        return Math.max(seconds, 1);
    }
}