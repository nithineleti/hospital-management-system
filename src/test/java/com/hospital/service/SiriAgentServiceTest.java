package com.hospital.service;

import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SiriAgentServiceTest {

    private DoctorService doctorService;
    private PatientService patientService;
    private AppointmentService appointmentService;
    private GeminiService geminiService;
    private HospitalStaffService hospitalStaffService;
    private SiriAgentService siriAgentService;

    @BeforeEach
    void setUp() {
        doctorService = Mockito.mock(DoctorService.class);
        patientService = Mockito.mock(PatientService.class);
        appointmentService = Mockito.mock(AppointmentService.class);
        geminiService = Mockito.mock(GeminiService.class);
        hospitalStaffService = Mockito.mock(HospitalStaffService.class);

        // Always return none for tool intent pipeline by default
        when(geminiService.generateResponse(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("{\"tool\":\"none\",\"arguments\":{}} ");

        siriAgentService = new SiriAgentService(doctorService, patientService, appointmentService, geminiService, hospitalStaffService);
    }

    @Test
    void handleNaturalLanguage_listDoctors_returnsDoctorNames() {
        Doctor dr = new Doctor();
        dr.setFirstName("James");
        dr.setLastName("Wilson");
        dr.setSpecialization("Cardiology");

        when(doctorService.getAllDoctors()).thenReturn(List.of(dr));

        String response = siriAgentService.handleNaturalLanguage("list doctors");

        assertThat(response).contains("James Wilson");
        assertThat(response).contains("Cardiology");
    }

    @Test
    void handleNaturalLanguage_findPatient_returnsPatient() {
        Patient patient = new Patient();
        patient.setFirstName("Alice");
        patient.setLastName("Smith");
        patient.setPhoneNumber("1234567890");
        patient.setStatus("ACTIVE");

        when(patientService.searchPatientByFirstName("Alice")).thenReturn(List.of(patient));

        String response = siriAgentService.handleNaturalLanguage("find patient Alice");

        assertThat(response).contains("Alice Smith");
        assertThat(response).contains("ACTIVE");
    }
}
