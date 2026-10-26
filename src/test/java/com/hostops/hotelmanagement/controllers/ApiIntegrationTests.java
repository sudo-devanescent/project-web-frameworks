package com.hostops.hotelmanagement.controllers;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTests {

	private static final String ADMINISTRADOR = "jvidaurre";
	private static final String RECEPCIONISTA = "rsantos";
	private static final String CONTRASENA = "HostOps2026";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void laApiProtegidaDevuelveProblemDetailSinSesion() throws Exception {
		mockMvc.perform(get("/api/habitaciones"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("No autenticado"));
	}

	@Test
	void elLoginCsrfYLogoutMantienenLaSesion() throws Exception {
		SesionApi sesion = iniciarSesion(ADMINISTRADOR);

		mockMvc.perform(get("/api/auth/me").session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombreUsuario").value(ADMINISTRADOR))
				.andExpect(jsonPath("$.rol").value("administrador"));

		mockMvc.perform(post("/api/auth/logout")
				.session(sesion.httpSession())
				.cookie(sesion.csrfCookie())
				.header(sesion.csrfHeader(), sesion.csrfToken()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(sesion.httpSession()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unAdministradorPuedeGestionarUsuariosYUnRecepcionistaNo() throws Exception {
		SesionApi administrador = iniciarSesion(ADMINISTRADOR);
		SesionApi recepcionista = iniciarSesion(RECEPCIONISTA);

		mockMvc.perform(get("/api/usuarios").session(administrador.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").exists());

		mockMvc.perform(get("/api/usuarios").session(recepcionista.httpSession()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON));

		mockMvc.perform(get("/api/habitaciones").session(recepcionista.httpSession()))
				.andExpect(status().isOk());
	}

	@Test
	void seValidaElCsrfYLosDatosDeEntrada() throws Exception {
		SesionApi administrador = iniciarSesion(ADMINISTRADOR);
		String numero = "T" + (System.nanoTime() % 100_000_000L);
		String cuerpo = """
				{"numero":"%s","tipo":"simple","capacidad":1,"tarifa":100.00}
				""".formatted(numero);

		mockMvc.perform(post("/api/habitaciones")
				.session(administrador.httpSession())
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/habitaciones")
				.session(administrador.httpSession())
				.cookie(administrador.csrfCookie())
				.header(administrador.csrfHeader(), administrador.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.numero").value(numero));

		mockMvc.perform(post("/api/habitaciones")
				.session(administrador.httpSession())
				.cookie(administrador.csrfCookie())
				.header(administrador.csrfHeader(), administrador.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"numero":"","tipo":"simple","capacidad":0,"tarifa":-1.00}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores", hasKey("numero")))
				.andExpect(jsonPath("$.errores", hasKey("capacidad")))
				.andExpect(jsonPath("$.errores", hasKey("tarifa")));
	}

	@Test
	void losListadosYResumenesRespondenConLaSesionActiva() throws Exception {
		SesionApi sesion = iniciarSesion(ADMINISTRADOR);

		mockMvc.perform(get("/api/detalles-reserva")
				.param("reservaId", "1")
				.session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/detalles-reserva")
				.param("estado", "confirmada")
				.session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/reservas/1/saldo").session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idReserva").value(1));

		mockMvc.perform(get("/api/pagos").param("limit", "5")
				.session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/finanzas/resumen").session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuentas").isArray())
				.andExpect(jsonPath("$.movimientos").isArray());

		mockMvc.perform(get("/api/dashboard/resumen").session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalHabitaciones").isNumber())
				.andExpect(jsonPath("$.llegadasPendientes").isArray());

		mockMvc.perform(get("/api/reportes/noches-ocupadas-por-mes")
				.session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/huespedes").param("q", "a")
				.session(sesion.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/pagos").param("limit", "0")
				.session(sesion.httpSession()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON));

		mockMvc.perform(get("/api/detalles-reserva").param("estado", "inexistente")
				.session(sesion.httpSession()))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/ruta-inexistente").session(sesion.httpSession()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"));

		mockMvc.perform(get("/api/auth/login").session(sesion.httpSession()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Método no permitido"));
	}

	@Test
	void seRechazaUnTipoDeContenidoNoSoportado() throws Exception {
		SesionApi administrador = iniciarSesion(ADMINISTRADOR);

		mockMvc.perform(post("/api/habitaciones")
				.session(administrador.httpSession())
				.cookie(administrador.csrfCookie())
				.header(administrador.csrfHeader(), administrador.csrfToken())
				.contentType(MediaType.TEXT_PLAIN)
				.content("numero=T1"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void unRecepcionistaNoPuedeCrearHabitaciones() throws Exception {
		SesionApi recepcionista = iniciarSesion(RECEPCIONISTA);

		mockMvc.perform(post("/api/habitaciones")
				.session(recepcionista.httpSession())
				.cookie(recepcionista.csrfCookie())
				.header(recepcionista.csrfHeader(), recepcionista.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"numero":"R1","tipo":"simple","capacidad":1,"tarifa":100.00}
						"""))
				.andExpect(status().isForbidden());
	}


	@Test
	void recepcionistaGestionaEstadosYBloqueosPeroNoCompensaciones() throws Exception {
		SesionApi administrador = iniciarSesion(ADMINISTRADOR);
		SesionApi recepcionista = iniciarSesion(RECEPCIONISTA);
		String numero = "C" + (System.nanoTime() % 100_000_000L);

		MvcResult creada = mockMvc.perform(post("/api/habitaciones")
				.session(administrador.httpSession())
				.cookie(administrador.csrfCookie())
				.header(administrador.csrfHeader(), administrador.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"numero":"%s","tipo":"simple","capacidad":1,"tarifa":100.00}
						""".formatted(numero)))
				.andExpect(status().isCreated())
				.andReturn();
		Integer idHabitacion = JsonPath.read(cuerpo(creada), "$.idHabitacion");

		mockMvc.perform(patch("/api/habitaciones/{id}/estado", idHabitacion)
				.session(recepcionista.httpSession())
				.cookie(recepcionista.csrfCookie())
				.header(recepcionista.csrfHeader(), recepcionista.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"estado":"mantenimiento"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("mantenimiento"));

		LocalDate hoy = LocalDate.now();
		MvcResult bloqueoCreado = mockMvc.perform(
				post("/api/habitaciones/{id}/bloqueos", idHabitacion)
						.session(recepcionista.httpSession())
						.cookie(recepcionista.csrfCookie())
						.header(recepcionista.csrfHeader(), recepcionista.csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fechaInicio":"%s","fechaFin":"%s","motivo":"Prueba"}
								""".formatted(hoy, hoy.plusDays(1))))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();
		Integer idBloqueo = JsonPath.read(cuerpo(bloqueoCreado), "$.idBloqueo");

		mockMvc.perform(get("/api/bloqueos/{id}", idBloqueo)
				.session(recepcionista.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idBloqueo").value(idBloqueo));

		mockMvc.perform(delete("/api/bloqueos/{id}", idBloqueo)
				.session(recepcionista.httpSession())
				.cookie(recepcionista.csrfCookie())
				.header(recepcionista.csrfHeader(), recepcionista.csrfToken()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/bloqueos/{id}", idBloqueo)
				.session(recepcionista.httpSession()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(
						MediaType.APPLICATION_PROBLEM_JSON));

		mockMvc.perform(get("/api/pagos/1").session(recepcionista.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idPago").value(1));
		mockMvc.perform(get("/api/canales/1").session(recepcionista.httpSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.idCanal").value(1));

		mockMvc.perform(post("/api/pagos/1/compensacion")
				.session(recepcionista.httpSession())
				.cookie(recepcionista.csrfCookie())
				.header(recepcionista.csrfHeader(), recepcionista.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"metodo":"efectivo","monto":1.00}
						"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/reservas/1/pagos")
				.session(recepcionista.httpSession())
				.cookie(recepcionista.csrfCookie())
				.header(recepcionista.csrfHeader(), recepcionista.csrfToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"reembolso","metodo":"efectivo","monto":1.00}
						"""))
				.andExpect(status().isForbidden());
	}

	private SesionApi iniciarSesion(String nombreUsuario) throws Exception {
		MockHttpSession sesionInicial = new MockHttpSession();
		MvcResult csrf = mockMvc.perform(get("/api/auth/csrf").session(sesionInicial))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andReturn();
		String token = JsonPath.read(cuerpo(csrf), "$.token");
		Cookie cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(cookie, "La respuesta CSRF debe crear la cookie XSRF-TOKEN");

		MvcResult login = mockMvc.perform(post("/api/auth/login")
				.session(sesionInicial)
				.cookie(cookie)
				.header("X-XSRF-TOKEN", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombreUsuario":"%s","contrasena":"%s"}
						""".formatted(nombreUsuario, CONTRASENA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombreUsuario").value(nombreUsuario))
				.andReturn();

		return new SesionApi(
				(MockHttpSession) login.getRequest().getSession(false),
				cookie,
				"X-XSRF-TOKEN",
				token);
	}

	private String cuerpo(MvcResult resultado) throws Exception {
		return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private record SesionApi(MockHttpSession httpSession, Cookie csrfCookie,
			String csrfHeader, String csrfToken) {
	}

}
