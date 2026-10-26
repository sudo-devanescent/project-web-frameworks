package com.hostops.hotelmanagement.services;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostops.hotelmanagement.models.EstadoDetalle;
import com.hostops.hotelmanagement.models.EstadoHabitacion;
import com.hostops.hotelmanagement.models.MetodoPago;
import com.hostops.hotelmanagement.models.TipoHabitacion;
import com.hostops.hotelmanagement.models.TipoMovimientoPago;
import com.hostops.hotelmanagement.services.dto.BloqueoHabitacionDto;
import com.hostops.hotelmanagement.services.dto.CanalDto;
import com.hostops.hotelmanagement.services.dto.CancelacionDto;
import com.hostops.hotelmanagement.services.dto.CuentaReservaDto;
import com.hostops.hotelmanagement.services.dto.DashboardResumenDto;
import com.hostops.hotelmanagement.services.dto.DetalleReservaDto;
import com.hostops.hotelmanagement.services.dto.DetalleReservaListadoDto;
import com.hostops.hotelmanagement.services.dto.EstadiaDto;
import com.hostops.hotelmanagement.services.dto.FinanzasResumenDto;
import com.hostops.hotelmanagement.services.dto.HabitacionDto;
import com.hostops.hotelmanagement.services.dto.HuespedDto;
import com.hostops.hotelmanagement.services.dto.NochesOcupadasPorMesDto;
import com.hostops.hotelmanagement.services.dto.OcupanteDto;
import com.hostops.hotelmanagement.services.dto.PagoDto;
import com.hostops.hotelmanagement.services.dto.ReservaCompletaDto;
import com.hostops.hotelmanagement.services.dto.ReservaDto;
import com.hostops.hotelmanagement.services.dto.RolDto;
import com.hostops.hotelmanagement.services.dto.SaldoReservaDto;
import com.hostops.hotelmanagement.services.exception.RecursoNoEncontradoException;

/**
 * Consultas de lectura específicas de la API, con joins y proyecciones planas.
 */
@Service
@Transactional(readOnly = true)
public class ConsultasApiService {

	private static final String SELECT_RESERVA = """
			select r.id_reserva, r.id_huesped_titular,
			       hu.nombres || ' ' || hu.apellidos as titular,
			       r.id_canal, c.nombre as canal,
			       r.id_usuario_creador, uc.nombre_completo as creador,
			       r.id_usuario_modificador, um.nombre_completo as modificador,
			       r.fecha_creacion, r.fecha_modificacion
			  from reserva r
			  join huesped hu on hu.id_huesped = r.id_huesped_titular
			  join canal c on c.id_canal = r.id_canal
			  join usuario uc on uc.id_usuario = r.id_usuario_creador
			  left join usuario um on um.id_usuario = r.id_usuario_modificador
			""";

	private static final String SELECT_DETALLE_LISTADO = """
			select d.id_detalle, d.id_reserva, d.id_habitacion, hab.numero as numero_habitacion,
			       r.id_huesped_titular,
			       hu.nombres || ' ' || hu.apellidos as titular,
			       r.id_canal, c.nombre as canal,
			       d.fecha_entrada_prevista, d.fecha_salida_prevista, d.estado,
			       coalesce(v.saldo_pendiente, 0) as saldo_pendiente
			  from detalle_reserva d
			  join reserva r on r.id_reserva = d.id_reserva
			  join huesped hu on hu.id_huesped = r.id_huesped_titular
			  join canal c on c.id_canal = r.id_canal
			  join habitacion hab on hab.id_habitacion = d.id_habitacion
			  left join v_saldo_reserva v on v.id_reserva = d.id_reserva
			""";

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public ConsultasApiService(JdbcTemplate jdbcTemplate, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	public List<RolDto> listarRoles() {
		return jdbcTemplate.query("""
				select r.id_rol, r.nombre, count(u.id_usuario) as usuarios
				  from rol r
				  left join usuario u on u.id_rol = r.id_rol
				 group by r.id_rol, r.nombre
				 order by r.nombre
				""", (rs, fila) -> new RolDto(
						rs.getInt("id_rol"),
						rs.getString("nombre"),
						rs.getLong("usuarios")));
	}

	public List<CanalDto> listarCanales() {
		return jdbcTemplate.query("""
				select c.id_canal, c.nombre, count(r.id_reserva) as reservas
				  from canal c
				  left join reserva r on r.id_canal = c.id_canal
				 group by c.id_canal, c.nombre
				 order by c.nombre
				""", (rs, fila) -> new CanalDto(
						rs.getInt("id_canal"),
						rs.getString("nombre"),
						rs.getLong("reservas")));
	}

	public CanalDto obtenerCanal(Integer idCanal) {
		return jdbcTemplate.query("""
				select c.id_canal, c.nombre, count(r.id_reserva) as reservas
				  from canal c
				  left join reserva r on r.id_canal = c.id_canal
				 where c.id_canal = ?
				 group by c.id_canal, c.nombre
				""", (rs, fila) -> new CanalDto(
						rs.getInt("id_canal"),
						rs.getString("nombre"),
						rs.getLong("reservas")), idCanal).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el canal indicado."));
	}

	public List<HabitacionDto> listarHabitaciones() {
		return jdbcTemplate.query("""
				select id_habitacion, numero, tipo, capacidad, tarifa, estado
				  from habitacion
				 order by numero
				""", (rs, fila) -> mapearHabitacion(rs));
	}

	public HabitacionDto obtenerHabitacion(Integer idHabitacion) {
		return jdbcTemplate.query("""
				select id_habitacion, numero, tipo, capacidad, tarifa, estado
				  from habitacion
				 where id_habitacion = ?
				""", (rs, fila) -> mapearHabitacion(rs), idHabitacion).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe la habitación indicada."));
	}

	public List<BloqueoHabitacionDto> listarBloqueos(Integer idHabitacion) {
		List<Object> parametros = new ArrayList<>();
		StringBuilder sql = new StringBuilder("""
				select b.id_bloqueo, b.id_habitacion, h.numero as numero_habitacion,
				       b.fecha_inicio, b.fecha_fin, b.motivo
				  from bloqueo_habitacion b
				  join habitacion h on h.id_habitacion = b.id_habitacion
				""");
		if (idHabitacion != null) {
			sql.append(" where b.id_habitacion = ?");
			parametros.add(idHabitacion);
		}
		sql.append(" order by b.fecha_inicio, b.id_bloqueo");
		return jdbcTemplate.query(sql.toString(), (rs, fila) -> new BloqueoHabitacionDto(
				rs.getInt("id_bloqueo"),
				rs.getInt("id_habitacion"),
				rs.getString("numero_habitacion"),
				rs.getObject("fecha_inicio", LocalDate.class),
				rs.getObject("fecha_fin", LocalDate.class),
				rs.getString("motivo")), parametros.toArray());
	}

	public BloqueoHabitacionDto obtenerBloqueo(Integer idBloqueo) {
		return jdbcTemplate.query("""
				select b.id_bloqueo, b.id_habitacion, h.numero as numero_habitacion,
				       b.fecha_inicio, b.fecha_fin, b.motivo
				  from bloqueo_habitacion b
				  join habitacion h on h.id_habitacion = b.id_habitacion
				 where b.id_bloqueo = ?
				""", (rs, fila) -> new BloqueoHabitacionDto(
						rs.getInt("id_bloqueo"),
						rs.getInt("id_habitacion"),
						rs.getString("numero_habitacion"),
						rs.getObject("fecha_inicio", LocalDate.class),
						rs.getObject("fecha_fin", LocalDate.class),
						rs.getString("motivo")), idBloqueo).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el bloqueo indicado."));
	}

	public List<HuespedDto> listarHuespedes(String texto) {
		String filtro = texto == null || texto.isBlank()
				? null
				: "%" + texto.trim().toLowerCase() + "%";
		StringBuilder sql = new StringBuilder("""
				select h.id_huesped, h.tipo_documento, h.numero_documento,
				       h.nombres, h.apellidos, h.telefono, h.correo,
				       h.nacionalidad, h.fecha_nacimiento, h.preferencias,
				       (select count(distinct d.id_detalle)
				          from detalle_reserva d
				          join reserva r on r.id_reserva = d.id_reserva
				         where d.estado <> 'cancelada'
				           and (r.id_huesped_titular = h.id_huesped
				                or exists (select 1 from detalle_huesped dh
				                            where dh.id_detalle = d.id_detalle
				                              and dh.id_huesped = h.id_huesped))) as estadias
				  from huesped h
				""");
		List<Object> parametros = new ArrayList<>();
		if (filtro != null) {
			sql.append("""
					 where lower(h.nombres || ' ' || h.apellidos) like ?
					    or lower(h.numero_documento) like ?
					""");
			parametros.add(filtro);
			parametros.add(filtro);
		}
		sql.append(" order by h.apellidos, h.nombres");
		return jdbcTemplate.query(sql.toString(),
				(rs, fila) -> mapearHuesped(rs), parametros.toArray());
	}

	public HuespedDto obtenerHuesped(Integer idHuesped) {
		return jdbcTemplate.query("""
				select h.id_huesped, h.tipo_documento, h.numero_documento,
				       h.nombres, h.apellidos, h.telefono, h.correo,
				       h.nacionalidad, h.fecha_nacimiento, h.preferencias,
				       (select count(distinct d.id_detalle)
				          from detalle_reserva d
				          join reserva r on r.id_reserva = d.id_reserva
				         where d.estado <> 'cancelada'
				           and (r.id_huesped_titular = h.id_huesped
				                or exists (select 1 from detalle_huesped dh
				                            where dh.id_detalle = d.id_detalle
				                              and dh.id_huesped = h.id_huesped))) as estadias
				  from huesped h
				 where h.id_huesped = ?
				""", (rs, fila) -> mapearHuesped(rs), idHuesped).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el huésped indicado."));
	}

	public List<EstadiaDto> listarEstadiasHuesped(Integer idHuesped) {
		return jdbcTemplate.query("""
				select distinct d.id_reserva, d.id_habitacion, hab.numero as numero_habitacion,
				       d.fecha_entrada_prevista, d.fecha_salida_prevista, d.estado
				  from detalle_reserva d
				  join reserva r on r.id_reserva = d.id_reserva
				  join habitacion hab on hab.id_habitacion = d.id_habitacion
				 where r.id_huesped_titular = ?
				    or exists (select 1 from detalle_huesped dh
				                where dh.id_detalle = d.id_detalle
				                  and dh.id_huesped = ?)
				 order by d.fecha_entrada_prevista desc, d.id_reserva desc
				""", (rs, fila) -> new EstadiaDto(
						rs.getInt("id_reserva"),
						rs.getInt("id_habitacion"),
						rs.getString("numero_habitacion"),
						rs.getObject("fecha_entrada_prevista", LocalDate.class),
						rs.getObject("fecha_salida_prevista", LocalDate.class),
						EstadoDetalle.valueOf(rs.getString("estado"))), idHuesped, idHuesped);
	}

	public List<DetalleReservaListadoDto> listarDetalles(Integer idReserva,
			Integer idHabitacion, EstadoDetalle estado) {
		List<Object> parametros = new ArrayList<>();
		StringBuilder sql = new StringBuilder(SELECT_DETALLE_LISTADO);
		List<String> condiciones = new ArrayList<>();
		if (idReserva != null) {
			condiciones.add("d.id_reserva = ?");
			parametros.add(idReserva);
		}
		if (idHabitacion != null) {
			condiciones.add("d.id_habitacion = ?");
			parametros.add(idHabitacion);
		}
		if (estado != null) {
			condiciones.add("d.estado = cast(? as estado_detalle)");
			parametros.add(estado.name());
		}
		if (!condiciones.isEmpty()) {
			sql.append(" where ").append(String.join(" and ", condiciones));
		}
		sql.append(" order by d.fecha_entrada_prevista, d.id_detalle");
		return jdbcTemplate.query(sql.toString(),
				(rs, fila) -> mapearDetalleListado(rs), parametros.toArray());
	}

	public DetalleReservaDto obtenerDetalle(Integer idDetalle) {
		List<DetalleCabecera> cabeceras = jdbcTemplate.query("""
				select d.id_detalle, d.id_reserva, d.id_habitacion, hab.numero as numero_habitacion,
				       hab.tipo as tipo_habitacion, hab.capacidad as capacidad_habitacion,
				       hab.estado as estado_habitacion,
				       d.fecha_entrada_prevista, d.fecha_salida_prevista,
				       d.checkin_real, d.checkout_real, d.tarifa_noche, d.estado,
				       coalesce(v.saldo_pendiente, 0) as saldo_pendiente
				  from detalle_reserva d
				  join habitacion hab on hab.id_habitacion = d.id_habitacion
				  left join v_saldo_reserva v on v.id_reserva = d.id_reserva
				 where d.id_detalle = ?
				""", (rs, fila) -> new DetalleCabecera(
						rs.getInt("id_detalle"),
						rs.getInt("id_reserva"),
						rs.getInt("id_habitacion"),
						rs.getString("numero_habitacion"),
						TipoHabitacion.valueOf(rs.getString("tipo_habitacion")),
						rs.getShort("capacidad_habitacion"),
						EstadoHabitacion.valueOf(rs.getString("estado_habitacion")),
						rs.getObject("fecha_entrada_prevista", LocalDate.class),
						rs.getObject("fecha_salida_prevista", LocalDate.class),
						instante(rs, "checkin_real"),
						instante(rs, "checkout_real"),
						rs.getBigDecimal("tarifa_noche"),
						EstadoDetalle.valueOf(rs.getString("estado")),
						rs.getBigDecimal("saldo_pendiente")), idDetalle);
		if (cabeceras.isEmpty()) {
			throw new RecursoNoEncontradoException("No existe el detalle indicado.");
		}

		DetalleCabecera cabecera = cabeceras.getFirst();
		List<OcupanteDto> ocupantes = jdbcTemplate.query("""
				select hu.id_huesped, hu.nombres || ' ' || hu.apellidos as nombre_completo
				  from detalle_huesped dh
				  join huesped hu on hu.id_huesped = dh.id_huesped
				 where dh.id_detalle = ?
				 order by hu.apellidos, hu.nombres
				""", (rs, fila) -> new OcupanteDto(
						rs.getInt("id_huesped"),
						rs.getString("nombre_completo")), idDetalle);
		return new DetalleReservaDto(
				cabecera.idDetalle(),
				obtenerReserva(cabecera.idReserva()),
				cabecera.idHabitacion(),
				cabecera.numeroHabitacion(),
				cabecera.tipoHabitacion(),
				cabecera.capacidadHabitacion(),
				cabecera.estadoHabitacion(),
				cabecera.fechaEntradaPrevista(),
				cabecera.fechaSalidaPrevista(),
				cabecera.checkinReal(),
				cabecera.checkoutReal(),
				cabecera.tarifaNoche(),
				cabecera.estado(),
				cabecera.saldoPendiente(),
				ocupantes);
	}

	public ReservaDto obtenerReserva(Integer idReserva) {
		return jdbcTemplate.query(SELECT_RESERVA + " where r.id_reserva = ?",
				(rs, fila) -> mapearReserva(rs), idReserva).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe la reserva indicada."));
	}

	public ReservaCompletaDto obtenerReservaCompleta(Integer idReserva) {
		ReservaDto reserva = obtenerReserva(idReserva);
		List<DetalleReservaDto> detalles = listarDetalles(idReserva, null, null).stream()
				.map(detalle -> obtenerDetalle(detalle.idDetalle()))
				.toList();
		return new ReservaCompletaDto(reserva, detalles);
	}

	public List<PagoDto> listarPagosReserva(Integer idReserva) {
		return jdbcTemplate.query("""
				select id_pago, id_reserva, id_usuario, id_pago_original, tipo, metodo, fecha_pago, monto, motivo
				  from pago
				 where id_reserva = ?
				 order by fecha_pago, id_pago
				""", (rs, fila) -> mapearPago(rs), idReserva);
	}

	public List<PagoDto> listarPagosRecientes(int limite) {
		return jdbcTemplate.query("""
				select id_pago, id_reserva, id_usuario, id_pago_original, tipo, metodo, fecha_pago, monto, motivo
				  from pago
				 order by fecha_pago desc, id_pago desc
				 limit ?
				""", (rs, fila) -> mapearPago(rs), limite);
	}

	public PagoDto obtenerPago(Integer idPago) {
		return jdbcTemplate.query("""
				select id_pago, id_reserva, id_usuario, id_pago_original, tipo, metodo, fecha_pago, monto, motivo
				  from pago
				 where id_pago = ?
				""", (rs, fila) -> mapearPago(rs), idPago).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el pago indicado."));
	}

	public CancelacionDto obtenerCancelacion(Integer idDetalle) {
		return jdbcTemplate.query("""
				select id_cancelacion, id_detalle, id_usuario, fecha_cancelacion, motivo, penalidad
				  from cancelacion
				 where id_detalle = ?
				""", (rs, fila) -> new CancelacionDto(
						rs.getInt("id_cancelacion"),
						rs.getInt("id_detalle"),
						rs.getInt("id_usuario"),
						instante(rs, "fecha_cancelacion"),
						rs.getString("motivo"),
						rs.getBigDecimal("penalidad")), idDetalle).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe una cancelación para el detalle indicado."));
	}

	public SaldoReservaDto obtenerSaldo(Integer idReserva) {
		return jdbcTemplate.query("""
				select id_reserva, total_alojamiento, total_penalidades, total_a_pagar,
				       cobrado_neto, saldo_pendiente, reembolso_pendiente
				  from v_saldo_reserva
				 where id_reserva = ?
				""", (rs, fila) -> new SaldoReservaDto(
						rs.getInt("id_reserva"),
						rs.getBigDecimal("total_alojamiento"),
						rs.getBigDecimal("total_penalidades"),
						rs.getBigDecimal("total_a_pagar"),
						rs.getBigDecimal("cobrado_neto"),
						rs.getBigDecimal("saldo_pendiente"),
						rs.getBigDecimal("reembolso_pendiente")), idReserva).stream()
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No se pudo calcular el saldo de la reserva."));
	}

	public List<NochesOcupadasPorMesDto> listarNochesOcupadasPorMes() {
		return jdbcTemplate.query("""
				select mes, noches_ocupadas
				  from v_noches_ocupadas_por_mes
				 order by mes
				""", (rs, fila) -> new NochesOcupadasPorMesDto(
						rs.getDate("mes").toLocalDate(),
						rs.getLong("noches_ocupadas")));
	}

	public FinanzasResumenDto resumenFinanzas() {
		List<CuentaReservaDto> cuentas = listarCuentas();
		BigDecimal saldos = cuentas.stream()
				.map(CuentaReservaDto::saldoPendiente)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal reembolsos = cuentas.stream()
				.map(CuentaReservaDto::reembolsoPendiente)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new FinanzasResumenDto(
				cobradoNetoMes(),
				saldos,
				reembolsos,
				cuentas,
				listarPagosRecientes(12));
	}

	public DashboardResumenDto resumenDashboard() {
		ContadoresHabitacion contadores = jdbcTemplate.queryForObject("""
				select count(*) as total,
				       count(*) filter (where estado = 'ocupada') as ocupadas,
				       count(*) filter (where estado = 'disponible') as disponibles,
				       count(*) filter (where estado = 'limpieza') as limpieza,
				       count(*) filter (where estado = 'mantenimiento') as mantenimiento
				  from habitacion
				""", (rs, fila) -> new ContadoresHabitacion(
						rs.getLong("total"),
						rs.getLong("ocupadas"),
						rs.getLong("disponibles"),
						rs.getLong("limpieza"),
						rs.getLong("mantenimiento")));

		Long reservasActivas = jdbcTemplate.queryForObject("""
				select count(distinct id_reserva)
				  from detalle_reserva
				 where estado in ('confirmada', 'en_estadia')
				""", Long.class);
		LocalDate hoy = LocalDate.now(clock);
		List<DetalleReservaListadoDto> llegadas = jdbcTemplate.query(
				SELECT_DETALLE_LISTADO + """
						 where d.estado = 'confirmada'
						   and d.fecha_entrada_prevista <= ?
						 order by d.fecha_entrada_prevista, d.id_detalle
						""", (rs, fila) -> mapearDetalleListado(rs), hoy);
		List<DetalleReservaListadoDto> salidas = jdbcTemplate.query(
				SELECT_DETALLE_LISTADO + """
						 where d.estado = 'en_estadia'
						   and d.fecha_salida_prevista <= ?
						 order by d.fecha_salida_prevista, d.id_detalle
						""", (rs, fila) -> mapearDetalleListado(rs), hoy);
		List<CuentaReservaDto> cuentasPendientes = listarCuentas().stream()
				.filter(cuenta -> cuenta.saldoPendiente().compareTo(BigDecimal.ZERO) > 0)
				.toList();
		BigDecimal saldos = cuentasPendientes.stream()
				.map(CuentaReservaDto::saldoPendiente)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new DashboardResumenDto(
				contadores.total(),
				contadores.ocupadas(),
				contadores.disponibles(),
				contadores.limpieza(),
				contadores.mantenimiento(),
				reservasActivas == null ? 0 : reservasActivas,
				cobradoNetoMes(),
				saldos,
				llegadas,
				salidas,
				cuentasPendientes);
	}

	private List<CuentaReservaDto> listarCuentas() {
		return jdbcTemplate.query("""
				select v.id_reserva, hu.nombres || ' ' || hu.apellidos as titular,
				       v.total_alojamiento, v.total_penalidades, v.total_a_pagar,
				       v.cobrado_neto, v.saldo_pendiente, v.reembolso_pendiente
				  from v_saldo_reserva v
				  join reserva r on r.id_reserva = v.id_reserva
				  join huesped hu on hu.id_huesped = r.id_huesped_titular
				 order by v.id_reserva
				""", (rs, fila) -> new CuentaReservaDto(
						rs.getInt("id_reserva"),
						rs.getString("titular"),
						rs.getBigDecimal("total_alojamiento"),
						rs.getBigDecimal("total_penalidades"),
						rs.getBigDecimal("total_a_pagar"),
						rs.getBigDecimal("cobrado_neto"),
						rs.getBigDecimal("saldo_pendiente"),
						rs.getBigDecimal("reembolso_pendiente")));
	}

	private BigDecimal cobradoNetoMes() {
		YearMonth mes = YearMonth.now(clock);
		Instant inicio = mes.atDay(1).atStartOfDay(clock.getZone()).toInstant();
		Instant fin = mes.plusMonths(1).atDay(1)
				.atStartOfDay(clock.getZone()).toInstant();
		BigDecimal total = jdbcTemplate.queryForObject("""
				select coalesce(sum(case when tipo = 'reembolso' then -monto else monto end), 0)
				  from pago
				 where fecha_pago >= ? and fecha_pago < ?
				""", BigDecimal.class, Timestamp.from(inicio), Timestamp.from(fin));
		return total == null ? BigDecimal.ZERO : total;
	}

	private HabitacionDto mapearHabitacion(ResultSet rs) throws SQLException {
		return new HabitacionDto(
				rs.getInt("id_habitacion"),
				rs.getString("numero"),
				TipoHabitacion.valueOf(rs.getString("tipo")),
				rs.getShort("capacidad"),
				rs.getBigDecimal("tarifa"),
				EstadoHabitacion.valueOf(rs.getString("estado")));
	}

	private HuespedDto mapearHuesped(ResultSet rs) throws SQLException {
		return new HuespedDto(
				rs.getInt("id_huesped"),
				rs.getString("tipo_documento"),
				rs.getString("numero_documento"),
				rs.getString("nombres"),
				rs.getString("apellidos"),
				rs.getString("telefono"),
				rs.getString("correo"),
				rs.getString("nacionalidad"),
				rs.getObject("fecha_nacimiento", LocalDate.class),
				rs.getString("preferencias"),
				rs.getLong("estadias"));
	}

	private ReservaDto mapearReserva(ResultSet rs) throws SQLException {
		return new ReservaDto(
				rs.getInt("id_reserva"),
				rs.getInt("id_huesped_titular"),
				rs.getString("titular"),
				rs.getInt("id_canal"),
				rs.getString("canal"),
				rs.getInt("id_usuario_creador"),
				rs.getString("creador"),
				entero(rs, "id_usuario_modificador"),
				rs.getString("modificador"),
				instante(rs, "fecha_creacion"),
				instante(rs, "fecha_modificacion"));
	}

	private DetalleReservaListadoDto mapearDetalleListado(ResultSet rs) throws SQLException {
		return new DetalleReservaListadoDto(
				rs.getInt("id_detalle"),
				rs.getInt("id_reserva"),
				rs.getInt("id_habitacion"),
				rs.getString("numero_habitacion"),
				rs.getInt("id_huesped_titular"),
				rs.getString("titular"),
				rs.getInt("id_canal"),
				rs.getString("canal"),
				rs.getObject("fecha_entrada_prevista", LocalDate.class),
				rs.getObject("fecha_salida_prevista", LocalDate.class),
				EstadoDetalle.valueOf(rs.getString("estado")),
				rs.getBigDecimal("saldo_pendiente"));
	}

	private PagoDto mapearPago(ResultSet rs) throws SQLException {
		return new PagoDto(
				rs.getInt("id_pago"),
				rs.getInt("id_reserva"),
				rs.getInt("id_usuario"),
				entero(rs, "id_pago_original"),
				TipoMovimientoPago.valueOf(rs.getString("tipo")),
				MetodoPago.valueOf(rs.getString("metodo")),
				instante(rs, "fecha_pago"),
				rs.getBigDecimal("monto"),
				rs.getString("motivo"));
	}

	private Integer entero(ResultSet rs, String columna) throws SQLException {
		int valor = rs.getInt(columna);
		return rs.wasNull() ? null : valor;
	}

	private Instant instante(ResultSet rs, String columna) throws SQLException {
		var fecha = rs.getTimestamp(columna);
		return fecha == null ? null : fecha.toInstant();
	}

	private record DetalleCabecera(
			Integer idDetalle,
			Integer idReserva,
			Integer idHabitacion,
			String numeroHabitacion,
			TipoHabitacion tipoHabitacion,
			Short capacidadHabitacion,
			EstadoHabitacion estadoHabitacion,
			LocalDate fechaEntradaPrevista,
			LocalDate fechaSalidaPrevista,
			Instant checkinReal,
			Instant checkoutReal,
			BigDecimal tarifaNoche,
			EstadoDetalle estado,
			BigDecimal saldoPendiente) {
	}

	private record ContadoresHabitacion(
			long total,
			long ocupadas,
			long disponibles,
			long limpieza,
			long mantenimiento) {
	}

}
