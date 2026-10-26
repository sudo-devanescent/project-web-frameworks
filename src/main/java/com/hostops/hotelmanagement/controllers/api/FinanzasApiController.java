package com.hostops.hotelmanagement.controllers.api;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostops.hotelmanagement.services.ConsultasApiService;
import com.hostops.hotelmanagement.services.dto.DashboardResumenDto;
import com.hostops.hotelmanagement.services.dto.FinanzasResumenDto;
import com.hostops.hotelmanagement.services.dto.NochesOcupadasPorMesDto;

/**
 * Expone los resúmenes de finanzas, panel y reportes.
 */
@RestController
@RequestMapping("/api")
@Validated
public class FinanzasApiController {

	private final ConsultasApiService consultasApiService;

	public FinanzasApiController(ConsultasApiService consultasApiService) {
		this.consultasApiService = consultasApiService;
	}

	@GetMapping("/finanzas/resumen")
	public FinanzasResumenDto resumenFinanzas() {
		return consultasApiService.resumenFinanzas();
	}

	@GetMapping("/dashboard/resumen")
	public DashboardResumenDto resumenDashboard() {
		return consultasApiService.resumenDashboard();
	}

	@GetMapping("/reportes/noches-ocupadas-por-mes")
	public List<NochesOcupadasPorMesDto> nochesOcupadasPorMes() {
		return consultasApiService.listarNochesOcupadasPorMes();
	}

}
