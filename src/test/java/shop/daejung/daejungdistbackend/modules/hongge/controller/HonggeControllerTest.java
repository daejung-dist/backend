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
                .andExpect(jsonPath("$[1].name").value("대정 실속형 홍게"));
    }

    @Test
    void getHonggeById() throws Exception {
        mockMvc.perform(get("/api/v1/hongges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("대정 특급 홍게"));
    }
}
