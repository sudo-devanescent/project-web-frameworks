package com.hostops.hotelmanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class WebControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void laRaizEntregaLaPaginaPrincipal() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"));
	}

	@Test
	void laRaizSirveElShellDeLaSpa() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"login\"")))
				.andExpect(content().string(containsString("id=\"shell\"")))
				.andExpect(content().string(containsString("id=\"app\"")))
				.andExpect(content().string(containsString("id=\"m\"")))
				.andExpect(content().string(containsString("id=\"tc\"")))
				.andExpect(content().string(containsString("css/styles.css")))
				.andExpect(content().string(containsString("js/app.js")));
	}

	@Test
	void laRaizNoEmiteLasEtiquetasDeThymeleaf() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("th:insert="))))
				.andExpect(content().string(not(containsString("th:fragment"))))
				.andExpect(content().string(not(containsString("th:replace="))))
				.andExpect(content().string(not(containsString("<th:block"))));
	}

	@Test
	void losEstaticosDeLaSpaEstanDisponibles() throws Exception {
		mockMvc.perform(get("/css/styles.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("@import")));
		mockMvc.perform(get("/css/tokens.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("--pri")));
		mockMvc.perform(get("/js/app.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Object.assign(window")));
		mockMvc.perform(get("/js/views/index.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("dashboard")));
	}

}
