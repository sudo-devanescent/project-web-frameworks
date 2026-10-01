package com.hostops.hotelmanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
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
	void laRaizInsertaLosFragmentosDeLasVistas() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"vista-login\"")))
				.andExpect(content().string(containsString("id=\"contenedor-interno\"")))
				.andExpect(content().string(containsString("id=\"form-login\"")))
				.andExpect(content().string(containsString("id=\"vista-dashboard\"")))
				.andExpect(content().string(containsString("id=\"vista-pagos\"")))
				.andExpect(content().string(containsString("id=\"cuerpo-tabla-habitaciones\"")))
				.andExpect(content().string(containsString("id=\"form-reserva\"")));
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
	void losFragmentosNoAnadenElementosAlDom() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(matchesPattern(compile(
						"(?s).*id=\"contenedor-interno\"[^>]*>\\s*<!--.*?-->\\s*<nav.*", DOTALL))))
				.andExpect(content().string(matchesPattern(compile(
						"(?s).*id=\"vista-login\"[^>]*>\\s*<!--.*?-->\\s*<header.*", DOTALL))));
	}

}