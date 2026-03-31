package com.hospital.controller;

import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.HospitalStaffService;
import com.hospital.service.PatientService;
import com.hospital.service.SiriAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AiAgentController {

    private final SiriAgentService siriAgentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final HospitalStaffService staffService;
    private final AppointmentService appointmentService;

    public AiAgentController(SiriAgentService siriAgentService, PatientService patientService, DoctorService doctorService,
                             HospitalStaffService staffService, AppointmentService appointmentService) {
        this.siriAgentService = siriAgentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.staffService = staffService;
        this.appointmentService = appointmentService;
    }

    @PostMapping("/api/ai/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("answer", "Question is required."));
        }

        String answer = siriAgentService.handleNaturalLanguage(question);
        return ResponseEntity.ok(Collections.singletonMap("answer", answer));
    }

    @GetMapping("/api/ai/dashboard")
    public ResponseEntity<Map<String,Object>> dashboardData() {
        Map<String,Object> data = new HashMap<>();
        data.put("patients", patientService.getAllPatients());
        data.put("doctors", doctorService.getAllDoctors());
        data.put("staff", staffService.getAllStaff());
        data.put("appointments", appointmentService.getAllAppointments());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/api/ai/entity/{type}/{id}")
    public ResponseEntity<Object> getEntityDetails(@org.springframework.web.bind.annotation.PathVariable String type,
                                                  @org.springframework.web.bind.annotation.PathVariable Long id) {
        switch (type.toLowerCase()) {
            case "patient":
                return patientService.getPatientById(id)
                    .map(p -> ResponseEntity.ok((Object) p))
                    .orElse(ResponseEntity.notFound().build());
            case "doctor":
                return doctorService.getDoctorById(id)
                    .map(d -> ResponseEntity.ok((Object) d))
                    .orElse(ResponseEntity.notFound().build());
            case "staff":
                return staffService.getStaffById(id)
                    .map(s -> ResponseEntity.ok((Object) s))
                    .orElse(ResponseEntity.notFound().build());
            case "appointment":
                return appointmentService.getAppointmentById(id)
                    .map(a -> ResponseEntity.ok((Object) a))
                    .orElse(ResponseEntity.notFound().build());
            default:
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Unknown entity type"));
        }
    }

    @GetMapping("/api/ai/export/{type}/{id}")
    public ResponseEntity<String> exportEntityDetails(@org.springframework.web.bind.annotation.PathVariable String type,
                                                      @org.springframework.web.bind.annotation.PathVariable Long id) {
        ResponseEntity<Object> details = getEntityDetails(type, id);
        if (!details.getStatusCode().is2xxSuccessful() || details.getBody() == null) {
            return ResponseEntity.status(details.getStatusCode()).body("Entity not found");
        }

        try {
            String content = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(details.getBody());
            String filename = type.toLowerCase() + "-details-" + id + ".json";
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Failed to build file");
        }
    }
}
