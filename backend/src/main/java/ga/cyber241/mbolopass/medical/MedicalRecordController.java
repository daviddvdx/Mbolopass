package ga.cyber241.mbolopass.medical;

import ga.cyber241.mbolopass.medical.MedicalDtos.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

public final class MedicalRecordController {
  private MedicalRecordController() {}

  @RestController
  @RequestMapping("/api/v1/patient")
  @PreAuthorize("hasRole('PATIENT')")
  public static class PatientMedicalRecordController {
    private final MedicalRecordService medicalRecords;

    public PatientMedicalRecordController(MedicalRecordService medicalRecords) {
      this.medicalRecords = medicalRecords;
    }

    @GetMapping("/medical-record")
    public MedicalRecordDto record(Principal principal) {
      return medicalRecords.ownRecord(principal.getName());
    }

    @GetMapping("/medical-access-requests")
    public List<MedicalAccessRequestDto> requests(Principal principal) {
      return medicalRecords.patientRequests(principal.getName());
    }

    @PostMapping("/medical-access-requests/{requestId}/approve")
    public MedicalAccessRequestDto approve(Principal principal, @PathVariable UUID requestId) {
      return medicalRecords.approveRequest(principal.getName(), requestId);
    }

    @PostMapping("/medical-access-requests/{requestId}/deny")
    public MedicalAccessRequestDto deny(Principal principal, @PathVariable UUID requestId) {
      return medicalRecords.denyRequest(principal.getName(), requestId);
    }

    @PostMapping("/medical-access-requests/{requestId}/temporary-code")
    public TemporaryAccessCodeResponse temporaryCode(Principal principal, @PathVariable UUID requestId) {
      return medicalRecords.generateTemporaryCode(principal.getName(), requestId);
    }

    @PostMapping("/medical-access-requests/{requestId}/revoke")
    public MedicalAccessRequestDto revoke(Principal principal, @PathVariable UUID requestId) {
      return medicalRecords.revokeAccess(principal.getName(), requestId);
    }
  }

  @RestController
  @RequestMapping("/api/v1/health-professionals")
  @PreAuthorize("hasRole('HEALTH_PROFESSIONAL')")
  public static class HealthProfessionalMedicalRecordController {
    private final MedicalRecordService medicalRecords;

    public HealthProfessionalMedicalRecordController(MedicalRecordService medicalRecords) {
      this.medicalRecords = medicalRecords;
    }

    @GetMapping("/patients")
    public PatientSearchPage patients(Principal principal, @RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
      return medicalRecords.searchPatients(principal.getName(), query, page, size);
    }

    @GetMapping("/medical-access-requests")
    public List<MedicalAccessRequestDto> requests(Principal principal) {
      return medicalRecords.professionalRequests(principal.getName());
    }

    @PostMapping("/medical-access-requests")
    public MedicalAccessRequestDto requestAccess(Principal principal, @Valid @RequestBody CreateMedicalAccessRequest request) {
      return medicalRecords.createAccessRequest(principal.getName(), request);
    }

    @PostMapping("/medical-access-requests/{requestId}/activate")
    public MedicalAccessRequestDto activate(Principal principal, @PathVariable UUID requestId, @Valid @RequestBody ActivateMedicalAccessRequest request) {
      return medicalRecords.activateRequest(principal.getName(), requestId, request);
    }

    @GetMapping("/patients/{patientId}/medical-record")
    public MedicalRecordDto patientRecord(Principal principal, @PathVariable UUID patientId) {
      return medicalRecords.professionalRecord(principal.getName(), patientId);
    }

    @PostMapping("/patients/{patientId}/medical-history")
    public MedicalHistoryEntryDto createHistory(Principal principal, @PathVariable UUID patientId, @Valid @RequestBody CreateMedicalHistoryEntryRequest request) {
      return medicalRecords.createHistory(principal.getName(), patientId, request);
    }

    @PutMapping("/patients/{patientId}/medical-history/{historyEntryId}")
    public MedicalHistoryEntryDto updateHistory(Principal principal, @PathVariable UUID patientId, @PathVariable UUID historyEntryId, @Valid @RequestBody UpdateMedicalHistoryEntryRequest request) {
      return medicalRecords.updateHistory(principal.getName(), patientId, historyEntryId, request);
    }

    @PostMapping("/patients/{patientId}/consultations")
    public MedicalConsultationDto createConsultation(Principal principal, @PathVariable UUID patientId, @Valid @RequestBody CreateMedicalConsultationRequest request) {
      return medicalRecords.createConsultation(principal.getName(), patientId, request);
    }

    @PutMapping("/patients/{patientId}/consultations/{consultationId}")
    public MedicalConsultationDto updateConsultation(Principal principal, @PathVariable UUID patientId, @PathVariable UUID consultationId, @Valid @RequestBody UpdateMedicalConsultationRequest request) {
      return medicalRecords.updateConsultation(principal.getName(), patientId, consultationId, request);
    }
  }
}
