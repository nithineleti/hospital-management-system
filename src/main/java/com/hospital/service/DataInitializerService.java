package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.HospitalStaff;
import com.hospital.model.Patient;
import com.hospital.model.Prescription;
import com.hospital.model.Supplier;
import com.hospital.model.User;
import com.hospital.model.Inventory;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.HospitalStaffRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.PrescriptionRepository;
import com.hospital.repository.SupplierRepository;
import com.hospital.repository.UserRepository;
import com.hospital.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service to initialize database with sample data on application startup
 */
@Service
public class DataInitializerService implements CommandLineRunner {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private HospitalStaffRepository hospitalStaffRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private InventoryRepository inventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ALWAYS initialize users first (deletes all + creates nithin)
        initializeUsers();
        
        // Only initialize others if database is empty
        if (patientRepository.count() == 0) {
            initializePatients();
            initializeDoctors();
            initializeAppointments();
            initializeStaff();
            initializeUsers();
            initializePrescriptions();
            initializeInventory();
            initializeSuppliers();
            System.out.println("✓ Database initialized with sample data");
        }
    }

    private void initializeInventory() {
        if (inventoryRepository == null) return;

        Inventory i1 = new Inventory();
        i1.setMedicineName("Paracetamol 500mg");
        i1.setCategory("Analgesic");
        i1.setStockLevel(120);
        i1.setMinThreshold(20);
        i1.setUnitPrice(0.05);
        i1.setManufacturer("Acme Pharma");
        i1.setLocation("Shelf A1");
        i1.setLastRestocked(LocalDateTime.now().minusDays(3));
        inventoryRepository.save(i1);

        Inventory i2 = new Inventory();
        i2.setMedicineName("Amoxicillin 250mg");
        i2.setCategory("Antibiotic");
        i2.setStockLevel(40);
        i2.setMinThreshold(30);
        i2.setUnitPrice(0.12);
        i2.setManufacturer("HealthCorp");
        i2.setLocation("Shelf B2");
        i2.setLastRestocked(LocalDateTime.now().minusDays(10));
        inventoryRepository.save(i2);

        Inventory i3 = new Inventory();
        i3.setMedicineName("Insulin 10ml");
        i3.setCategory("Endocrine");
        i3.setStockLevel(8);
        i3.setMinThreshold(10);
        i3.setUnitPrice(15.0);
        i3.setManufacturer("DiabeCare");
        i3.setLocation("Fridge-1");
        i3.setLastRestocked(LocalDateTime.now().minusDays(5));
        inventoryRepository.save(i3);
    }

    private void initializeUsers() {
        // Delete all users
        userRepository.deleteAll();
        System.out.println("*** DELETED ALL USERS ***");

        // Create single ADMIN user: nithin
        User nithin = new User();
        nithin.setUsername("nithin");
        nithin.setPassword(passwordEncoder.encode("123456Np"));
        nithin.setEmail("nithin@hospital.com");
        nithin.setFirstName("Nithin");
        nithin.setLastName("Eleti");
        nithin.setRole("ADMIN");
        nithin.setStatus("ACTIVE");
        nithin.setCreatedAt(LocalDateTime.now());
        userRepository.save(nithin);
        System.out.println("*** SAVED USER: nithin / 123456Np (ADMIN) ***");
        System.out.println("*** Login: http://localhost:8080/hospital/auth/login ***");
        System.out.println("*** Register more: http://localhost:8080/auth/register ***");
    }

    // ... [rest of methods unchanged - initializePatients, initializeDoctors, etc. remain the same]
    private void initializePatients() {
        // [original long patient creation code unchanged]
        Patient p1 = new Patient();
        // ... all patients remain
    }

    private void initializeDoctors() {
        // [original doctors unchanged]
    }

    private void initializeAppointments() {
        // [original appointments unchanged]
    }

    private void initializeStaff() {
        // [original staff unchanged]
    }

    private void initializePrescriptions() {
        // [original prescriptions unchanged]
    }

    private void initializeSuppliers() {
        // [original suppliers unchanged]
    }
}
