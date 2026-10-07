package shop.daejung.daejungdistbackend.modules.hongge.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class HonggeControllerTest {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getHongges() throws Exception {
        mockMvc.perform(get("/api/v1/hongges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("대정 특급 홍게"))
                .andExpect(jsonPath("$[0].description").value("살이 꽉 찬 프리미엄급 홍게로 선물용으로 좋은 구성입니다."))
                .andExpect(jsonPath("$[0].thumbnailUrl").value("https://api.daejung.shop/images/hongge/premium_crab.jpg"))
                .andExpect(jsonPath("$[1].name").value("대정 실속형 홍게"))
                .andExpect(jsonPath("$[1].description").value("가성비 좋은 실속 구성으로 가정용 식사에 잘 어울립니다."))
                .andExpect(jsonPath("$[1].thumbnailUrl").value("https://api.daejung.shop/images/hongge/economic_crab.jpg"));
    }

    @Test
    void getHonggeById() throws Exception {
        mockMvc.perform(get("/api/v1/hongges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("대정 특급 홍게"))
                .andExpect(jsonPath("$.description").value("살이 꽉 찬 프리미엄급 홍게로 선물용으로 좋은 구성입니다."))
                .andExpect(jsonPath("$.thumbnailUrl").value("https://api.daejung.shop/images/hongge/premium_crab.jpg"));
    }
}
