package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.model.HospitalStaff;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SiriAgentService {

    private final DoctorService doctorService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final GeminiService geminiService;
    private final HospitalStaffService hospitalStaffService;

    @Value("${gemini.agent.enabled:true}")
    private boolean geminiAgentEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SIRI_PERSONA = "You are SIRI, the Smart Intelligent Robotic Interface, acting as the hospital operations assistant. " +
            "Your responses must be calm, concise, empathetic, helpful, and professional. Use no more than 2 sentences when possible. " +
            "ALWAYS use hospital tools and data instead of making up patient/doctor info. If no data is found, ask the user for more details.";

    private static final String SIRI_TOOL_PROMPT = "You are SIRI, the medical assistant. For every user request, return a valid JSON object only (no extra text) with fields: {\"tool\": <toolName>, \"arguments\": {...}}. " +
            "tool can be: bookAppointment, updateDiagnosis, findPatient, findDoctor, none. " +
            "For bookAppointment fill patientName, doctorName, dateTime. " +
            "For updateDiagnosis fill patientName, diagnosis. If unable to parse set tool to none.";

    public SiriAgentService(DoctorService doctorService, PatientService patientService, AppointmentService appointmentService, GeminiService geminiService, HospitalStaffService hospitalStaffService) {
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.geminiService = geminiService;
        this.hospitalStaffService = hospitalStaffService;
    }

    public String handleNaturalLanguage(String userInput) {
        // Structured tool route first
        String structuredResult = runToolPipeline(userInput);
        if (structuredResult != null && !structuredResult.isEmpty()) {
            return structuredResult;
        }

        String lower = userInput.toLowerCase(Locale.ROOT);

        // Direct local intent-handling first
        if (lower.contains("list doctors") || lower.contains("show doctors") || lower.contains("find doctor")) {
            return listDoctors(userInput);
        }

        if (lower.contains("list patients") || lower.contains("find patient") || lower.contains("patient record")) {
            return findPatients(userInput);
        }

        if (lower.contains("room") && (lower.contains("patient") || lower.contains("room number"))) {
            return findPatientRoom(userInput);
        }

        if (lower.contains("book appointment") || lower.contains("schedule appointment") || lower.contains("new appointment")) {
            return scheduleAppointment(userInput);
        }

        if (lower.contains("update diagnosis") || lower.contains("set diagnosis") || lower.contains("add diagnosis")) {
            return updateDiagnosis(userInput);
        }

        if (lower.contains("list staff") || lower.contains("staff details")) {
            return listStaff(userInput);
        }

        if (lower.contains("find staff")) {
            return findStaff(userInput);
        }

        // Fallback to Gemini for open natural language response
        if (geminiAgentEnabled) {
            return geminiService.generateResponse(SIRI_PERSONA, userInput);
        }

        return "I could not understand your request. Please provide more detail, like a patient name or doctor specialisation.";
    }

    private String listDoctors(String input) {
        List<Doctor> doctors = doctorService.getAllDoctors();
        if (doctors.isEmpty()) {
            return "No doctors are currently registered in the system. Please add a doctor or check the hospital roster.";
        }
        return doctors.stream()
                .limit(5)
                .map(d -> String.format("%s %s: %s", d.getFirstName(), d.getLastName(), d.getSpecialization()))
                .collect(Collectors.joining("; "));
    }

    private String findPatients(String input) {
        // crude name extraction: look for name parts in input
        String[] words = input.split("\\s+");
        for (String w : words) {
            if (w.length() > 2) {
                List<Patient> byFirst = patientService.searchPatientByFirstName(w);
                if (!byFirst.isEmpty()) {
                    return formatPatients(byFirst);
                }
                List<Patient> byLast = patientService.searchPatientByLastName(w);
                if (!byLast.isEmpty()) {
                    return formatPatients(byLast);
                }
            }
        }

        return "I could not find a matching patient name. Please provide full first and/or last name.";
    }

    private String formatPatients(List<Patient> patients) {
        return patients.stream()
                .limit(5)
                .map(p -> String.format("%s %s, %s, status %s", p.getFirstName(), p.getLastName(), p.getPhoneNumber(), p.getStatus()))
                .collect(Collectors.joining("; "));
    }

    private String findPatientRoom(String input) {
        // if room number is stored somewhere else, use appointment/patient data model; not available in current model
        return "Room data is not available in this module. Please use the dedicated room assignment screen or provide patient full name for exact lookup.";
    }

    private String scheduleAppointment(String input) {
        // patterns: for patient {name} with doctor {name} on {yyyy-MM-dd[ HH:mm]} (simple matching)
        Pattern pattern = Pattern.compile("patient\\s+([A-Za-z ]+?)\\s+with\\s+doctor\\s+([A-Za-z ]+?)\\s+on\\s+([0-9- :]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String patientName = matcher.group(1).trim();
            String doctorName = matcher.group(2).trim();
            String dateTimeText = matcher.group(3).trim();

            Patient patient = findPatientByName(patientName);
            if (patient == null) {
                return "Patient '" + patientName + "' not found. Please provide full first and/or last name.";
            }

            Doctor doctor = findDoctorByName(doctorName);
            if (doctor == null) {
                return "Doctor '" + doctorName + "' not found. Please provide a valid doctor name.";
            }

            LocalDateTime appointmentDateTime;
            try {
                appointmentDateTime = LocalDateTime.parse(dateTimeText, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception e) {
                try {
                    appointmentDateTime = LocalDateTime.parse(dateTimeText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception ex) {
                    return "Date/time format not recognized. Please use 'YYYY-MM-DD' or 'YYYY-MM-DD HH:mm'.";
                }
            }

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setAppointmentDateTime(appointmentDateTime);
            appointment.setStatus("SCHEDULED");
            appointment.setCreatedAt(LocalDateTime.now());
            appointment.setReason("Routine appointment requested by SIRI");

            appointmentService.createAppointment(appointment);
            return "Appointment scheduled for " + patient.getFirstName() + " " + patient.getLastName() + " with Dr. " + doctor.getLastName() + " on " + appointmentDateTime + ".";
        }

        return "To schedule, say: 'Book appointment for patient <name> with doctor <name> on YYYY-MM-DD or YYYY-MM-DD HH:mm'.";
    }

    private Patient findPatientByName(String patientName) {
        String[] parts = patientName.split(" ");
        for (String part : parts) {
            if (part.length() < 2) continue;
            List<Patient> first = patientService.searchPatientByFirstName(part);
            if (!first.isEmpty()) return first.get(0);
            List<Patient> last = patientService.searchPatientByLastName(part);
            if (!last.isEmpty()) return last.get(0);
        }
        return null;
    }

    private Doctor findDoctorByName(String doctorName) {
        String[] parts = doctorName.split(" ");
        for (String part : parts) {
            if (part.length() < 2) continue;
            List<Doctor> first = doctorService.getDoctorsByFirstName(part);
            if (!first.isEmpty()) return first.get(0);
            List<Doctor> last = doctorService.getDoctorsByLastName(part);
            if (!last.isEmpty()) return last.get(0);
        }
        return null;
    }

    private String updateDiagnosis(String input) {
        // format: update diagnosis for patient <name> to <diagnosis>
        Pattern diagPattern = Pattern.compile("update diagnosis for patient\\s+([A-Za-z ]+?)\\s+to\\s+(.+)$", Pattern.CASE_INSENSITIVE);
        Matcher diagMatcher = diagPattern.matcher(input);
        if (diagMatcher.find()) {
            String patientName = diagMatcher.group(1).trim();
            String diagnosis = diagMatcher.group(2).trim();

            Patient patient = findPatientByName(patientName);
            if (patient == null) {
                return "Patient '" + patientName + "' not found. Provide the full name.";
            }

            List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patient);
            if (appointments.isEmpty()) {
                return "No existing appointment found for " + patient.getFirstName() + " " + patient.getLastName() + ". Please create one first.";
            }
            Appointment appointment = appointments.get(0);
            appointment.setNotes("Diagnosis updated by SIRI: " + diagnosis);
            appointment.setStatus("DIAGNOSED");
            appointmentService.updateAppointment(appointment);
            return "Diagnosis updated for " + patient.getFirstName() + " " + patient.getLastName() + ".";
        }

        return "Use format: 'Update diagnosis for patient <name> to <diagnosis>'.";
    }

    private String listStaff(String input) {
        List<HospitalStaff> staff = hospitalStaffService.getAllStaff();
        if (staff.isEmpty()) {
            return "No staff members are currently registered in the system.";
        }
        return staff.stream()
                .limit(5)
                .map(s -> String.format("%s %s (%s, %s)", s.getFirstName(), s.getLastName(), s.getPosition(), s.getDepartment()))
                .collect(Collectors.joining("; "));
    }

    private String findStaff(String input) {
        String[] words = input.split("\\s+");
        for (String w : words) {
            if (w.length() > 2) {
                List<com.hospital.model.HospitalStaff> byFirst = hospitalStaffService.searchStaffByFirstName(w);
                if (!byFirst.isEmpty()) {
                    return byFirst.stream().limit(5)
                            .map(s -> String.format("%s %s, %s, %s", s.getFirstName(), s.getLastName(), s.getPosition(), s.getPhoneNumber()))
                            .collect(Collectors.joining("; "));
                }
            }
        }
        return "No staff member found matching that query. Please try a first or last name.";
    }

    private String runToolPipeline(String userInput) {
        try {
            String analysis = geminiService.generateResponse(SIRI_TOOL_PROMPT, userInput);
            JsonNode parsed = objectMapper.readTree(analysis);
            String tool = parsed.path("tool").asText("none");
            JsonNode args = parsed.path("arguments");

            switch (tool) {
                case "bookAppointment":
                    String pName = args.path("patientName").asText();
                    String dName = args.path("doctorName").asText();
                    String dt = args.path("dateTime").asText();
                    if (!pName.isBlank() && !dName.isBlank() && !dt.isBlank()) {
                        return scheduleAppointmentDirect(pName, dName, dt);
                    }
                    break;
                case "updateDiagnosis":
                    String patientName = args.path("patientName").asText();
                    String diagnosis = args.path("diagnosis").asText();
                    if (!patientName.isBlank() && !diagnosis.isBlank()) {
                        return updateDiagnosisDirect(patientName, diagnosis);
                    }
                    break;
                case "findPatient":
                    return findPatients(userInput);
                case "findDoctor":
                    return listDoctors(userInput);
                default:
                    return null;
            }
        } catch (Exception e) {
            // fallback to standard text pipeline if parsing fails
            return null;
        }
        return null;
    }

    private String scheduleAppointmentDirect(String patientName, String doctorName, String dateTimeText) {
        Patient patient = findPatientByName(patientName);
        if (patient == null) {
            return "Patient '" + patientName + "' not found. Please provide complete name.";
        }
        Doctor doctor = findDoctorByName(doctorName);
        if (doctor == null) {
            return "Doctor '" + doctorName + "' not found. Please provide complete doctor name.";
        }
        LocalDateTime appointmentDateTime;
        try {
            appointmentDateTime = LocalDateTime.parse(dateTimeText, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ex) {
            try {
                appointmentDateTime = LocalDateTime.parse(dateTimeText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ex2) {
                return "Date/time format not recognized. Use YYYY-MM-DD or YYYY-MM-DD HH:mm.";
            }
        }
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus("SCHEDULED");
        appointment.setReason("Scheduled via SIRI");
        appointment.setCreatedAt(LocalDateTime.now());
        appointmentService.createAppointment(appointment);
        return "Appointment scheduled for " + patient.getFirstName() + " " + patient.getLastName() + " with Dr. " + doctor.getLastName() + " on " + appointmentDateTime + ".";
    }

    private String updateDiagnosisDirect(String patientName, String diagnosis) {
        Patient patient = findPatientByName(patientName);
        if (patient == null) {
            return "Patient '" + patientName + "' not found for diagnosis update.";
        }
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patient);
        if (appointments.isEmpty()) {
            return "No appointment found for patient " + patient.getFirstName() + " " + patient.getLastName() + ".";
        }
        Appointment appointment = appointments.get(0);
        appointment.setNotes("Diagnosis updated via SIRI: " + diagnosis);
        appointment.setStatus("DIAGNOSED");
        appointmentService.updateAppointment(appointment);
        return "Diagnosis for " + patient.getFirstName() + " " + patient.getLastName() + " has been updated to: " + diagnosis + ".";
    }
}
