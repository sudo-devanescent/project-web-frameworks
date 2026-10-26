package com.hostops.hotelmanagement.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define el reloj de negocio en la zona horaria configurada.
 */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock(@Value("${hostops.zona-horaria:America/Lima}") String zonaHoraria) {
		return Clock.system(ZoneId.of(zonaHoraria));
	}

}
