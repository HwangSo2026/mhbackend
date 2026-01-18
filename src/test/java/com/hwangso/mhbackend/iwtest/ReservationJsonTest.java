package com.hwangso.mhbackend.iwtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReservationJsonTest {

    static class Reservation {
        public String name;
        public String coures;
        public int headCount;

        // Jackson이 기본 생성자를 필요로 할 수도 있음
        public Reservation() {
        }

        public Reservation(String name, String coures, int headCount) {
            this.coures = coures;
            this.name = name;
            this.headCount = headCount;
        }
    }

    @Test
    @DisplayName("역직렬화 테스트")
    void fromJson_deserialize_success() throws Exception {

        //given
        ObjectMapper om = new ObjectMapper();
        String json = """
                {
                    "name":"인우",
                    "coures":"웹/앱",
                    "headCount": 4
                }                 
                """;

        // when
        Reservation r = om.readValue(json, Reservation.class);

        //then
        System.out.println("r = " + r);
        System.out.println("r.name = " + r.name);
        System.out.println("r.coures = " + r.coures);
        System.out.println("r.headCount = " + r.headCount);

        Assertions.assertEquals("인우", r.name);
        Assertions.assertEquals("웹/앱", r.coures);
        Assertions.assertEquals(4, r.headCount);

    }

}
