package com.hostops.hotelmanagement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.Canal;
import com.hostops.hotelmanagement.models.DetalleReserva;
import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.Habitacion;
import com.hostops.hotelmanagement.models.Huesped;
import com.hostops.hotelmanagement.models.MetodoPago;
import com.hostops.hotelmanagement.models.Pago;
import com.hostops.hotelmanagement.models.Reserva;
import com.hostops.hotelmanagement.models.Rol;
import com.hostops.hotelmanagement.models.TipoHabitacion;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;
import com.hostops.hotelmanagement.models.Usuario;
import com.hostops.hotelmanagement.repositories.CanalRepository;
import com.hostops.hotelmanagement.repositories.RolRepository;
import com.hostops.hotelmanagement.repositories.UsuarioRepository;
import com.hostops.hotelmanagement.repositories.VistasConsultaRepository;
import com.hostops.hotelmanagement.services.dto.CancelacionComando;
import com.hostops.hotelmanagement.services.dto.CompensacionPagoComando;
import com.hostops.hotelmanagement.services.dto.DetalleReservaComando;
import com.hostops.hotelmanagement.services.dto.HabitacionComando;
import com.hostops.hotelmanagement.services.dto.HuespedComando;
import com.hostops.hotelmanagement.services.dto.NochesOcupadasPorMesDto;
import com.hostops.hotelmanagement.services.dto.PagoComando;
import com.hostops.hotelmanagement.services.dto.ReservaActualizarComando;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaCrearComando;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaDto;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaEditarComando;
import com.hostops.hotelmanagement.services.dto.ReservaCrearComando;
import com.hostops.hotelmanagement.services.dto.ReservaLineaComando;
import com.hostops.hotelmanagement.services.dto.SaldoReservaDto;
import com.hostops.hotelmanagement.services.dto.UsuarioComando;
import com.hostops.hotelmanagement.services.dto.UsuarioDto;
import com.hostops.hotelmanagement.services.exception.ConflictoException;
import com.hostops.hotelmanagement.services.exception.ReglaNegocioException;
import com.hostops.hotelmanagement.services.exception.SaldoPendienteException;

@SpringBootTest
@Transactional
class ServicesIntegrationTests {

	@Autowired
	private RolRepository rolRepository;
	@Autowired
	private CanalRepository canalRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private HabitacionService habitacionService;
	@Autowired
	private BloqueoHabitacionService bloqueoHabitacionService;
	@Autowired
	private HuespedService huespedService;
	@Autowired
	private ReservaService reservaService;
	@Autowired
	private DetalleReservaService detalleReservaService;
	@Autowired
	private CancelacionService cancelacionService;
	@Autowired
	private PagoService pagoService;
	@Autowired
	private VistasConsultaRepository vistasConsultaRepository;
	@Autowired
	private ReservaOperacionesService reservaOperacionesService;
	@Autowired
	private ConsultasApiService consultasApiService;

	@Test
	void elUsuarioCreadoNoExponeElHashYSeProtegeElUltimoAdministrador() {
		Rol administrador = rol("administrador");
		for (Usuario usuario : usuarioRepository.findAll()) {
			if (administrador.getNombre().equalsIgnoreCase(usuario.getRol().getNombre())) {
				usuario.setActivo(false);
			}
		}
		usuarioRepository.flush();

		String sufijo = sufijo();
		UsuarioDto creado = usuarioService.crear(new UsuarioComando(
				administrador.getIdRol(), "admin_" + sufijo, "Administrador de Prueba",
				"admin_" + sufijo + "@hostops.test", "hash_de_prueba", null));
		assertNotNull(creado.idUsuario());
		assertEquals(Boolean.TRUE, creado.activo(), "activo debe tomar el DEFAULT de la BD");
		assertEquals(administrador.getNombre(), creado.nombreRol());

		UsuarioComando desactivar = new UsuarioComando(administrador.getIdRol(),
				creado.nombreUsuario(), creado.nombreCompleto(), creado.correo(), null,
				Boolean.FALSE);
		assertThrows(ReglaNegocioException.class,
				() -> usuarioService.actualizar(creado.idUsuario(), desactivar));
	}

	@Test
	void elFlujoDeReservaDetalleCancelacionYPagoMantieneElSaldo() {
		Huesped titular = crearHuesped("Titular");
		Huesped acompanante = crearHuesped("Acompanante");
		Habitacion habitacion = crearHabitacion((short) 2);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		Reserva reserva = reservaService.crear(new ReservaCrearComando(
				titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario()));
		DetalleReserva detalle = detalleReservaService.crear(reserva.getIdReserva(),
				new DetalleReservaComando(habitacion.getIdHabitacion(),
						LocalDate.of(2099, 12, 1), LocalDate.of(2099, 12, 3),
						new BigDecimal("100.00")));

		detalleReservaService.agregarHuesped(detalle.getIdDetalle(), titular.getIdHuesped());
		detalleReservaService.agregarHuesped(detalle.getIdDetalle(), acompanante.getIdHuesped());
		assertThrows(ReglaNegocioException.class, () -> detalleReservaService
				.eliminarHuesped(detalle.getIdDetalle(), titular.getIdHuesped()));

		cancelacionService.cancelar(detalle.getIdDetalle(), new CancelacionComando(
				"Cancelación de prueba", new BigDecimal("20.00"), creador.getIdUsuario()));

		SaldoReservaDto saldo = pagoService.obtenerSaldo(reserva.getIdReserva());
		assertEquals(0, BigDecimal.ZERO.compareTo(saldo.totalAlojamiento()));
		assertEquals(0, new BigDecimal("20.00").compareTo(saldo.totalAPagar()));
		assertEquals(0, new BigDecimal("20.00").compareTo(saldo.saldoPendiente()));

		assertThrows(SaldoPendienteException.class, () -> pagoService.registrar(
				reserva.getIdReserva(), new PagoComando(TipoMovimientoPago.anticipo,
						MetodoPago.efectivo,
						new BigDecimal("30.00"))));

		Pago pago = pagoService.registrar(reserva.getIdReserva(),
				new PagoComando(TipoMovimientoPago.anticipo,
						MetodoPago.efectivo,
						new BigDecimal("20.00"),
						creador.getIdUsuario(),
						"Pago de prueba"));
		assertEquals(0, BigDecimal.ZERO.compareTo(
				pagoService.obtenerSaldo(reserva.getIdReserva()).saldoPendiente()));

		assertEquals(creador.getIdUsuario(), pago.getUsuario().getIdUsuario());
		assertEquals("Pago de prueba", pago.getMotivo());
		Pago compensacion = pagoService.compensar(pago.getIdPago(),
				new CompensacionPagoComando(null, null, creador.getIdUsuario()));
		assertEquals(TipoMovimientoPago.reembolso, compensacion.getTipo());
		assertEquals(0, new BigDecimal("20.00").compareTo(compensacion.getMonto()));
		assertEquals(pago.getIdPago(), compensacion.getPagoOriginal().getIdPago());
		assertEquals(creador.getIdUsuario(), compensacion.getUsuario().getIdUsuario());
		assertNull(compensacion.getMotivo());
		assertEquals(0, new BigDecimal("20.00").compareTo(
				pagoService.obtenerSaldo(reserva.getIdReserva()).saldoPendiente()));
		assertThrows(ReglaNegocioException.class, () -> pagoService.compensar(
				pago.getIdPago(), new CompensacionPagoComando(null, null,
						creador.getIdUsuario())));
	}

	@Test
	void elTitularDebePermanecerEnUnaLineaYSeActualizaLaFechaDeModificacion() {
		Huesped titular = crearHuesped("Titular");
		Huesped nuevoTitular = crearHuesped("NuevoTitular");
		Habitacion habitacion = crearHabitacion((short) 2);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		Reserva reserva = reservaService.crear(new ReservaCrearComando(
				titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario()));
		DetalleReserva detalle = detalleReservaService.crear(reserva.getIdReserva(),
				new DetalleReservaComando(habitacion.getIdHabitacion(),
						LocalDate.of(2099, 10, 1), LocalDate.of(2099, 10, 3),
						new BigDecimal("90.00")));
		detalleReservaService.agregarHuesped(detalle.getIdDetalle(), titular.getIdHuesped());

		ReservaActualizarComando cambiarTitular = new ReservaActualizarComando(
				nuevoTitular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario());
		assertThrows(ReglaNegocioException.class,
				() -> reservaService.actualizar(reserva.getIdReserva(), cambiarTitular));

		detalleReservaService.agregarHuesped(detalle.getIdDetalle(), nuevoTitular.getIdHuesped());
		Reserva actualizada = reservaService.actualizar(reserva.getIdReserva(), cambiarTitular);
		assertNotNull(actualizada.getFechaModificacion());
	}

	@Test
	void seEliminaUnaReservaVaciaYUnDetalleSinHistorial() {
		Huesped titular = crearHuesped("Titular");
		Habitacion habitacion = crearHabitacion((short) 1);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		Reserva vacia = reservaService.crear(new ReservaCrearComando(
				titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario()));
		reservaService.eliminar(vacia.getIdReserva());

		Reserva reserva = reservaService.crear(new ReservaCrearComando(
				titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario()));
		DetalleReserva detalle = detalleReservaService.crear(reserva.getIdReserva(),
				new DetalleReservaComando(habitacion.getIdHabitacion(),
						LocalDate.of(2099, 9, 1), LocalDate.of(2099, 9, 2),
						new BigDecimal("80.00")));
		detalleReservaService.eliminar(detalle.getIdDetalle());
		reservaService.eliminar(reserva.getIdReserva());
	}

	@Test
	void lasVistasSeConsultaronConJdbcTemplate() {
		Huesped titular = crearHuesped("Titular");
		Habitacion habitacion = crearHabitacion((short) 1);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		Reserva reserva = reservaService.crear(new ReservaCrearComando(
				titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario()));
		DetalleReserva detalle = detalleReservaService.crear(reserva.getIdReserva(),
				new DetalleReservaComando(habitacion.getIdHabitacion(),
						LocalDate.of(2099, 11, 10), LocalDate.of(2099, 11, 13),
						new BigDecimal("70.00")));
		assertEquals(EstadoDetalle.confirmada, detalle.getEstado());

		SaldoReservaDto saldo = vistasConsultaRepository.obtenerSaldoReserva(reserva.getIdReserva())
				.orElseThrow();
		assertEquals(0, new BigDecimal("210.00").compareTo(saldo.totalAlojamiento()));

		List<NochesOcupadasPorMesDto> meses = vistasConsultaRepository
				.listarNochesOcupadasPorMes();
		NochesOcupadasPorMesDto noviembre = meses.stream()
				.filter(mes -> LocalDate.of(2099, 11, 1).equals(mes.mes()))
				.findFirst()
				.orElseThrow();
		assertTrue(noviembre.nochesOcupadas() >= 3);
	}

	@Test
	void laCreacionCompletaYEdicionDeReservaEsAtomica() {
		Huesped titular = crearHuesped("Titular");
		Huesped acompanante = crearHuesped("Acompanante");
		Habitacion primera = crearHabitacion((short) 2);
		Habitacion segunda = crearHabitacion((short) 1);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		ReservaCompletaDto creada = reservaOperacionesService.crear(
				new ReservaCompletaCrearComando(
						titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario(),
						List.of(
								new ReservaLineaComando(primera.getIdHabitacion(),
										LocalDate.of(2099, 12, 1), LocalDate.of(2099, 12, 3),
										List.of(titular.getIdHuesped(), acompanante.getIdHuesped())),
								new ReservaLineaComando(segunda.getIdHabitacion(),
										LocalDate.of(2099, 12, 3), LocalDate.of(2099, 12, 5),
										List.of(titular.getIdHuesped()))),
						MetodoPago.tarjeta, new BigDecimal("100.00")));

		assertEquals(2, creada.detalles().size());
		assertEquals(2, creada.detalles().getFirst().huespedes().size());
		SaldoReservaDto saldo = pagoService.obtenerSaldo(creada.reserva().idReserva());
		assertEquals(0, new BigDecimal("400.00").compareTo(saldo.totalAPagar()));
		assertEquals(0, new BigDecimal("300.00").compareTo(saldo.saldoPendiente()));

		ReservaCompletaDto editada = reservaOperacionesService.editar(
				creada.reserva().idReserva(), creada.detalles().get(1).idDetalle(),
				new ReservaCompletaEditarComando(
						titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario(),
						segunda.getIdHabitacion(), LocalDate.of(2099, 12, 4),
						LocalDate.of(2099, 12, 6), List.of(acompanante.getIdHuesped())));
		assertEquals(LocalDate.of(2099, 12, 4),
				editada.detalles().get(1).fechaEntradaPrevista());
		assertEquals(1, editada.detalles().get(1).huespedes().size());
	}

	@Test
	void laEdicionEnEstadiaSoloCambiaSalidaYOcupantes() {
		Huesped titular = crearHuesped("Titular");
		Huesped acompanante = crearHuesped("Acompanante");
		Habitacion habitacion = crearHabitacion((short) 2);
		Habitacion otra = crearHabitacion((short) 2);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();
		LocalDate hoy = LocalDate.now();

		ReservaCompletaDto creada = reservaOperacionesService.crear(
				new ReservaCompletaCrearComando(
						titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario(),
						List.of(new ReservaLineaComando(habitacion.getIdHabitacion(),
								hoy, hoy.plusDays(1), List.of(titular.getIdHuesped()))),
						null, null));
		Integer idDetalle = creada.detalles().getFirst().idDetalle();
		detalleReservaService.registrarCheckin(idDetalle);
		assertEquals(EstadoHabitacion.ocupada,
				consultasApiService.obtenerDetalle(idDetalle).estadoHabitacion());

		ReservaCompletaDto editada = reservaOperacionesService.editar(
				creada.reserva().idReserva(), idDetalle,
				new ReservaCompletaEditarComando(
						titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario(),
						habitacion.getIdHabitacion(), hoy, hoy.plusDays(3),
						List.of(titular.getIdHuesped(), acompanante.getIdHuesped())));
		assertEquals(hoy.plusDays(3),
				editada.detalles().getFirst().fechaSalidaPrevista());
		assertEquals(2, editada.detalles().getFirst().huespedes().size());

		assertThrows(ReglaNegocioException.class,
				() -> reservaOperacionesService.editar(
						creada.reserva().idReserva(), idDetalle,
						new ReservaCompletaEditarComando(
								titular.getIdHuesped(), canal.getIdCanal(),
								creador.getIdUsuario(), otra.getIdHabitacion(), hoy,
								hoy.plusDays(3), List.of(titular.getIdHuesped()))));
	}


	@Test
	void noSePuedeReducirLaCapacidadPorDebajoDeLosOcupantesActivos() {
		Huesped titular = crearHuesped("Titular");
		Huesped acompanante = crearHuesped("Acompanante");
		Habitacion habitacion = crearHabitacion((short) 2);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		Reserva reserva = reservaService.crear(new ReservaCrearComando(
				titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario()));
		DetalleReserva detalle = detalleReservaService.crear(reserva.getIdReserva(),
				new DetalleReservaComando(habitacion.getIdHabitacion(),
						LocalDate.of(2099, 8, 1), LocalDate.of(2099, 8, 3),
						new BigDecimal("100.00")));
		detalleReservaService.agregarHuesped(detalle.getIdDetalle(), titular.getIdHuesped());
		detalleReservaService.agregarHuesped(detalle.getIdDetalle(), acompanante.getIdHuesped());

		assertThrows(ConflictoException.class, () -> habitacionService.actualizar(
				habitacion.getIdHabitacion(),
				new HabitacionComando(habitacion.getNumero(), habitacion.getTipo(), (short) 1,
						habitacion.getTarifa(), null)));
	}

	@Test
	void elTitularPuedeEstarEnUnaLineaPosteriorAlCrearLaReserva() {
		Huesped titular = crearHuesped("Titular");
		Huesped acompanante = crearHuesped("Acompanante");
		Habitacion primera = crearHabitacion((short) 2);
		Habitacion segunda = crearHabitacion((short) 2);
		Usuario creador = crearUsuario("recepcionista");
		Canal canal = canalRepository.findByNombre("presencial").orElseThrow();

		ReservaCompletaDto creada = reservaOperacionesService.crear(
				new ReservaCompletaCrearComando(
						titular.getIdHuesped(), canal.getIdCanal(), creador.getIdUsuario(),
						List.of(
								new ReservaLineaComando(primera.getIdHabitacion(),
										LocalDate.of(2099, 7, 1), LocalDate.of(2099, 7, 2),
										List.of(acompanante.getIdHuesped())),
								new ReservaLineaComando(segunda.getIdHabitacion(),
										LocalDate.of(2099, 7, 1), LocalDate.of(2099, 7, 2),
										List.of(titular.getIdHuesped()))),
						null, null));

		assertEquals(2, creada.detalles().size());
	}

	@Test
	void losBloqueosNoModificanElEstadoManualDeLaHabitacion() {
		Habitacion habitacion = crearHabitacion((short) 1);
		LocalDate hoy = LocalDate.now();

		var bloqueo = bloqueoHabitacionService.crear(
				new com.hostops.hotelmanagement.services.dto.BloqueoHabitacionComando(
						habitacion.getIdHabitacion(), hoy, hoy.plusDays(1),
						"Bloqueo de prueba"));
		assertEquals(EstadoHabitacion.disponible,
				habitacionService.obtener(habitacion.getIdHabitacion()).getEstado());

		habitacionService.cambiarEstado(habitacion.getIdHabitacion(),
				EstadoHabitacion.mantenimiento);
		bloqueoHabitacionService.eliminar(bloqueo.getIdBloqueo());
		assertEquals(EstadoHabitacion.mantenimiento,
				habitacionService.obtener(habitacion.getIdHabitacion()).getEstado());
	}

	private Rol rol(String nombre) {
		return rolRepository.findByNombre(nombre).orElseThrow();
	}

	private Usuario crearUsuario(String nombreRol) {
		String sufijo = sufijo();
		Rol rol = rol(nombreRol);
		UsuarioDto creado = usuarioService.crear(new UsuarioComando(rol.getIdRol(),
				"usuario_" + sufijo, "Usuario " + sufijo, "usuario_" + sufijo + "@hostops.test",
				"hash_de_prueba", null));
		return usuarioRepository.findById(creado.idUsuario()).orElseThrow();
	}

	private Huesped crearHuesped(String etiqueta) {
		String sufijo = sufijo();
		return huespedService.crear(new HuespedComando("DNI", "DOC" + sufijo,
				etiqueta, "Prueba " + sufijo, "999888777", null, "PE",
				LocalDate.of(1990, 1, 1), "Sin preferencias"));
	}

	private Habitacion crearHabitacion(short capacidad) {
		String sufijo = sufijo();
		return habitacionService.crear(new HabitacionComando("Z" + sufijo,
				TipoHabitacion.doble, capacidad, new BigDecimal("100.00"), null));
	}

	private static String sufijo() {
		return String.format("%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
	}

}
