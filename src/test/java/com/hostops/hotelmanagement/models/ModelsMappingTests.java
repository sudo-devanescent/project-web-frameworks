package com.hostops.hotelmanagement.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Prueba de mapeo de las entidades JPA contra el esquema real de PostgreSQL.
 * Se ejecuta con la base de datos levantada y {@code ddl-auto=validate}.
 * La transacción de cada prueba se revierte (las secuencias no).
 */
@SpringBootTest
@Transactional
class ModelsMappingTests {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private javax.sql.DataSource dataSource;

	private static final List<Class<?>> ENTIDADES = List.of(
			Rol.class, Canal.class, Usuario.class, Habitacion.class,
			BloqueoHabitacion.class, Huesped.class, Reserva.class,
			DetalleReserva.class, DetalleHuesped.class, Cancelacion.class, Pago.class);

	@Test
	void laCoberturaDeTablasCoincideConLasEntidades() throws Exception {
		Set<String> tablasBd = new HashSet<>();
		try (var conn = dataSource.getConnection();
				var ps = conn.prepareStatement(
						"SELECT table_name FROM information_schema.tables "
								+ "WHERE table_schema='public' AND table_type='BASE TABLE'");
				var rs = ps.executeQuery()) {
			while (rs.next()) {
				tablasBd.add(rs.getString(1).toLowerCase());
			}
		}

		Set<String> tablasEntidades = ENTIDADES.stream()
				.map(c -> c.getAnnotation(jakarta.persistence.Table.class).name().toLowerCase())
				.collect(Collectors.toSet());

		assertEquals(tablasBd, tablasEntidades,
				"Las tablas base y las entidades mapeadas deben coincidir (ni tablas sin entidad ni entidades sin tabla)");
	}

	@Test
	void laLecturaDeCadaEntidadCoincideConElConteoNativo() {
		for (Class<?> entidad : ENTIDADES) {
			long jpql = em.createQuery("select count(e) from " + entidad.getSimpleName() + " e", Long.class)
					.getSingleResult();
			long nativo = ((Number) em.createNativeQuery(
					"select count(*) from " + tablaDe(entidad)).getSingleResult()).longValue();
			assertEquals(nativo, jpql, "Conteo distinto para " + entidad.getSimpleName());

			List<?> filas = em.createQuery("select e from " + entidad.getSimpleName() + " e", entidad)
					.getResultList();
			assertEquals(nativo, filas.size(), "No se cargaron todas las filas de " + entidad.getSimpleName());
		}
	}

	@Test
	void laEscrituraEnOrdenDeDependenciaFunciona() {
		Rol rol = rolNuevo();
		em.persist(rol);
		em.flush();
		assertNotNull(rol.getIdRol(), "Rol debe recibir id generado");

		Usuario creador = usuarioNuevo(rol);
		em.persist(creador);
		em.flush();
		assertNotNull(creador.getIdUsuario(), "Usuario debe recibir id generado");

		Canal canal = new Canal();
		canal.setNombre("canal_prueba_" + System.nanoTime());
		em.persist(canal);
		em.flush();
		assertNotNull(canal.getIdCanal(), "Canal debe recibir id generado");

		Huesped titular = huespedNuevo();
		em.persist(titular);
		em.flush();
		assertNotNull(titular.getIdHuesped(), "Huesped debe recibir id generado");

		Habitacion hab = habitacionNueva(TipoHabitacion.matrimonial, (short) 2, "300.00");
		em.persist(hab);
		em.flush();
		assertNotNull(hab.getIdHabitacion(), "Habitacion debe recibir id generado");

		Reserva reserva = new Reserva();
		reserva.setHuespedTitular(titular);
		reserva.setCanal(canal);
		reserva.setUsuarioCreador(creador);
		em.persist(reserva);
		em.flush();
		assertNotNull(reserva.getIdReserva(), "Reserva debe recibir id generado");

		DetalleReserva detalle = new DetalleReserva();
		detalle.setReserva(reserva);
		detalle.setHabitacion(hab);
		detalle.setFechaEntradaPrevista(LocalDate.of(2099, 3, 1));
		detalle.setFechaSalidaPrevista(LocalDate.of(2099, 3, 4));
		detalle.setTarifaNoche(new BigDecimal("300.00"));
		detalle.setEstado(EstadoDetalle.confirmada);
		em.persist(detalle);
		em.flush();
		assertNotNull(detalle.getIdDetalle(), "DetalleReserva debe recibir id generado");

		Integer idHuesped = titular.getIdHuesped();
		em.clear();

		Huesped recargado = em.find(Huesped.class, idHuesped);
		assertNotNull(recargado);
		assertEquals(titular.getNumeroDocumento(), recargado.getNumeroDocumento());
		assertEquals(titular.getNombres(), recargado.getNombres());
		assertEquals(titular.getFechaNacimiento(), recargado.getFechaNacimiento());
	}

	@Test
	void losEnumsSeEscribenYReleenIgual() {
		Habitacion hab = habitacionNueva(TipoHabitacion.suite, (short) 4, "800.00");
		em.persist(hab);
		em.flush();
		Integer idHab = hab.getIdHabitacion();
		em.clear();
		assertEquals(TipoHabitacion.suite, em.find(Habitacion.class, idHab).getTipo());
	}

	@Test
	void losDefaultsDeLaBaseDeDatosSeRellenanAlRefrescar() {
		Habitacion hab = habitacionNueva(TipoHabitacion.doble, (short) 2, "250.00");
		hab.setEstado(null);
		em.persist(hab);
		em.flush();
		Integer idHab = hab.getIdHabitacion();
		em.refresh(hab);
		assertEquals(EstadoHabitacion.disponible, hab.getEstado(), "estado debe tomar el DEFAULT");

		em.clear();
		assertEquals(EstadoHabitacion.disponible, em.find(Habitacion.class, idHab).getEstado());

		Rol rol = rolNuevo();
		em.persist(rol);
		Usuario usuario = usuarioNuevo(rol);
		usuario.setActivo(null);
		em.persist(usuario);
		em.flush();
		em.refresh(usuario);
		assertEquals(Boolean.TRUE, usuario.getActivo(), "activo debe tomar el DEFAULT");
	}

	@Test
	void laFechaDeModificacionLaEscribeElTrigger() {
		Rol rol = rolNuevo();
		em.persist(rol);
		Usuario creador = usuarioNuevo(rol);
		em.persist(creador);
		Canal canal = new Canal();
		canal.setNombre("canal_prueba_" + System.nanoTime());
		em.persist(canal);
		Huesped titular = huespedNuevo();
		em.persist(titular);
		em.flush();

		Reserva reserva = new Reserva();
		reserva.setHuespedTitular(titular);
		reserva.setCanal(canal);
		reserva.setUsuarioCreador(creador);
		reserva.setFechaCreacion(null);
		em.persist(reserva);
		em.flush();
		em.refresh(reserva);
		assertNotNull(reserva.getFechaCreacion(), "fecha_creacion debe tomar el DEFAULT");
		assertNull(reserva.getFechaModificacion(), "fecha_modificacion debe ser nula al insertar");

		reserva.setUsuarioModificador(creador);
		em.flush();
		em.refresh(reserva);
		assertNotNull(reserva.getFechaModificacion(), "fecha_modificacion debe rellenarla el trigger");
	}

	@Test
	void laClaveCompuestaDeDetalleHuespedSePersisteYRelee() {
		Rol rol = rolNuevo();
		em.persist(rol);
		Usuario creador = usuarioNuevo(rol);
		em.persist(creador);
		Canal canal = new Canal();
		canal.setNombre("canal_prueba_" + System.nanoTime());
		em.persist(canal);
		Huesped titular = huespedNuevo();
		em.persist(titular);
		Habitacion hab = habitacionNueva(TipoHabitacion.simple, (short) 1, "150.00");
		em.persist(hab);
		em.flush();

		Reserva reserva = new Reserva();
		reserva.setHuespedTitular(titular);
		reserva.setCanal(canal);
		reserva.setUsuarioCreador(creador);
		em.persist(reserva);
		em.flush();

		DetalleReserva detalle = new DetalleReserva();
		detalle.setReserva(reserva);
		detalle.setHabitacion(hab);
		detalle.setFechaEntradaPrevista(LocalDate.of(2099, 1, 10));
		detalle.setFechaSalidaPrevista(LocalDate.of(2099, 1, 12));
		detalle.setTarifaNoche(new BigDecimal("150.00"));
		detalle.setEstado(EstadoDetalle.confirmada);
		em.persist(detalle);
		em.flush();

		DetalleHuesped dh = new DetalleHuesped();
		dh.setId(new DetalleHuespedId(detalle.getIdDetalle(), titular.getIdHuesped()));
		dh.setDetalleReserva(detalle);
		dh.setHuesped(titular);
		em.persist(dh);
		em.flush();
		DetalleHuespedId id = dh.getId();
		em.clear();

		DetalleHuesped recargado = em.find(DetalleHuesped.class, id);
		assertNotNull(recargado, "Debe releerse por su clave compuesta");
		assertEquals(id, recargado.getId());
	}

	@Test
	void elTriggerDeCancelacionMarcaElDetalleComoCancelada() {
		Rol rol = rolNuevo();
		em.persist(rol);
		Usuario creador = usuarioNuevo(rol);
		em.persist(creador);
		Canal canal = new Canal();
		canal.setNombre("canal_prueba_" + System.nanoTime());
		em.persist(canal);
		Huesped titular = huespedNuevo();
		em.persist(titular);
		Habitacion hab = habitacionNueva(TipoHabitacion.triple, (short) 3, "400.00");
		em.persist(hab);
		em.flush();

		Reserva reserva = new Reserva();
		reserva.setHuespedTitular(titular);
		reserva.setCanal(canal);
		reserva.setUsuarioCreador(creador);
		em.persist(reserva);
		em.flush();

		DetalleReserva detalle = new DetalleReserva();
		detalle.setReserva(reserva);
		detalle.setHabitacion(hab);
		detalle.setFechaEntradaPrevista(LocalDate.of(2099, 2, 1));
		detalle.setFechaSalidaPrevista(LocalDate.of(2099, 2, 3));
		detalle.setTarifaNoche(new BigDecimal("400.00"));
		detalle.setEstado(EstadoDetalle.confirmada);
		em.persist(detalle);
		em.flush();

		Cancelacion cancelacion = new Cancelacion();
		cancelacion.setDetalleReserva(detalle);
		cancelacion.setUsuario(creador);
		cancelacion.setMotivo("Prueba de mapeo");
		cancelacion.setFechaCancelacion(null);
		cancelacion.setPenalidad(null);
		em.persist(cancelacion);
		em.flush();

		em.refresh(detalle);
		assertEquals(EstadoDetalle.cancelada, detalle.getEstado(),
				"El trigger debe marcar el detalle como cancelada");

		em.refresh(cancelacion);
		assertNotNull(cancelacion.getFechaCancelacion(), "fecha_cancelacion debe tomar el DEFAULT");
		assertNotNull(cancelacion.getPenalidad(), "penalidad debe tomar el DEFAULT");
		assertEquals(0, BigDecimal.ZERO.compareTo(cancelacion.getPenalidad()));
	}

	private static String tablaDe(Class<?> entidad) {
		return entidad.getAnnotation(jakarta.persistence.Table.class).name();
	}

	private Rol rolNuevo() {
		Rol rol = new Rol();
		rol.setNombre("rol_prueba_" + System.nanoTime());
		return rol;
	}

	private Usuario usuarioNuevo(Rol rol) {
		long n = System.nanoTime();
		Usuario usuario = new Usuario();
		usuario.setRol(rol);
		usuario.setNombreUsuario("usuario_prueba_" + n);
		usuario.setNombreCompleto("Prueba " + n);
		usuario.setCorreo("prueba_" + n + "@hostops.test");
		usuario.setContrasenaHash("hash_prueba");
		usuario.setActivo(true);
		return usuario;
	}

	private Huesped huespedNuevo() {
		long n = System.nanoTime();
		Huesped h = new Huesped();
		h.setTipoDocumento("DNI");
		h.setNumeroDocumento("DOC" + n);
		h.setNombres("Prueba");
		h.setApellidos("Mapeo");
		h.setTelefono("999888777");
		h.setNacionalidad("PE");
		h.setFechaNacimiento(LocalDate.of(1990, 1, 1));
		return h;
	}

	private Habitacion habitacionNueva(TipoHabitacion tipo, Short capacidad, String tarifa) {
		Habitacion hab = new Habitacion();
		hab.setNumero("T" + numeroCorto());
		hab.setTipo(tipo);
		hab.setCapacidad(capacidad);
		hab.setTarifa(new BigDecimal(tarifa));
		return hab;
	}

	private static String numeroCorto() {
		return String.format("%06d", Math.floorMod(System.nanoTime(), 1_000_000));
	}
}
