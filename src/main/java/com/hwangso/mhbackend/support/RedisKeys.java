package com.hwangso.mhbackend.support;

public class RedisKeys {

    /** 홀드 키 */
    /** ex) hold:2025-01-01:part3:1 */
    public static String holdKey(String date, String slot, String room) {
        return "hold:" + date + ":" + slot + ":" + room;
    }

    /** 예약 키 */
    /** ex) rsv:2025-01-01:part3 */
    public static String rsvKey(String date, String slot) {
        return "rsv:" + date + ":" + slot;
    }

    /** 관리자 키 */
    /** ex) admintoken:8f3c2a7e-4d1b-4b8f-9f2e-1c7a6c9d0b31 */
    public static String adminTokenKey(String token) {
        return "admintoken:" + token;
    }

}
