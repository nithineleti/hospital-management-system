package com.hospital.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SiriUIController {

    @GetMapping("/hospital/siri")
    public String siriUi(Model model) {
        model.addAttribute("agentName", "SIRI");
        return "siri";
    }
}
