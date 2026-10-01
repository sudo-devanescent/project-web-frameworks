package com.hostops.hotelmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

	/** Entrega la vista principal, renderizada por Thymeleaf. */
	@GetMapping("/")
	public String index() {
		return "index";
	}

}