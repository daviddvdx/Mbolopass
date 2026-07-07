package ga.cyber241.mbolopass.clinical;

import ga.cyber241.mbolopass.clinical.ClinicalService.*;
import ga.cyber241.mbolopass.common.Enums.ProfessionalVerificationStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/professional")
@PreAuthorize("hasRole('HEALTH_PROFESSIONAL')")
class ProfessionalController {
  private final ClinicalService service;
  ProfessionalController(ClinicalService service) { this.service = service; }

  @GetMapping("/me")
  ProfessionalMe me(Principal principal) { return service.professionalMe(principal.getName()); }

  @PostMapping("/access-requests")
  AccessResponse requestAccess(Principal principal, HttpServletRequest http, @Valid @RequestBody AccessRequest request) {
    return service.requestAccess(principal.getName(), request, http);
  }

  @GetMapping("/access-requests")
  List<AccessResponse> accessRequests(Principal principal) { return service.professionalAccesses(principal.getName()); }

  @GetMapping("/patients/{healthProfileId}/record")
  PatientRecord record(Principal principal, HttpServletRequest http, @PathVariable UUID healthProfileId) {
    return service.record(principal.getName(), healthProfileId, http);
  }

  @PostMapping("/patients/{healthProfileId}/encounters")
  EncounterResponse createEncounter(Principal principal, HttpServletRequest http, @PathVariable UUID healthProfileId, @RequestBody EncounterRequest request) {
    return service.createEncounter(principal.getName(), healthProfileId, request, http);
  }

  @GetMapping("/encounters/{encounterId}")
  EncounterResponse encounter(Principal principal, @PathVariable UUID encounterId) { return service.encounter(principal.getName(), encounterId); }

  @PatchMapping("/encounters/{encounterId}")
  EncounterResponse updateEncounter(Principal principal, HttpServletRequest http, @PathVariable UUID encounterId, @RequestBody EncounterRequest request) {
    return service.updateEncounter(principal.getName(), encounterId, request, http);
  }

  @PostMapping("/encounters/{encounterId}/close")
  EncounterResponse closeEncounter(Principal principal, HttpServletRequest http, @PathVariable UUID encounterId) {
    return service.closeEncounter(principal.getName(), encounterId, http);
  }

  @PostMapping("/encounters/{encounterId}/diagnoses")
  ClinicalItemResponse diagnosis(Principal principal, HttpServletRequest http, @PathVariable UUID encounterId, @RequestBody DiagnosisRequest request) {
    return service.createDiagnosis(principal.getName(), encounterId, request, http);
  }

  @PostMapping("/encounters/{encounterId}/allergies")
  ClinicalItemResponse allergy(Principal principal, HttpServletRequest http, @PathVariable UUID encounterId, @Valid @RequestBody AllergyRequest request) {
    return service.createAllergy(principal.getName(), encounterId, request, http);
  }

  @PostMapping("/encounters/{encounterId}/exam-orders")
  ExamOrderResponse examOrder(Principal principal, HttpServletRequest http, @PathVariable UUID encounterId, @RequestBody ExamOrderRequest request) {
    return service.createExamOrder(principal.getName(), encounterId, request, http);
  }

  @PostMapping("/exam-orders/{examOrderId}/results")
  ExamResultResponse result(Principal principal, HttpServletRequest http, @PathVariable UUID examOrderId, @Valid @RequestBody ExamResultRequest request) {
    return service.enterExamResult(principal.getName(), examOrderId, request, http);
  }

  @PostMapping("/exam-results/{examResultId}/validate")
  ExamResultResponse validateResult(Principal principal, HttpServletRequest http, @PathVariable UUID examResultId) {
    return service.validateExamResult(principal.getName(), examResultId, http);
  }

  @GetMapping("/medical-catalog")
  List<CatalogResponse> catalog() { return service.catalog(); }

  @GetMapping("/exam-catalog")
  List<ExamCatalogResponse> examCatalog() { return service.examCatalog(); }
}

@RestController
@RequestMapping("/api/v1/patient")
@PreAuthorize("hasRole('PATIENT')")
class PatientClinicalController {
  private final ClinicalService service;
  PatientClinicalController(ClinicalService service) { this.service = service; }
  @GetMapping("/access-requests")
  List<AccessResponse> accessRequests(Principal principal) { return service.patientAccesses(principal.getName()); }
  @PostMapping("/access-requests/{accessId}/approve")
  AccessResponse approve(Principal principal, HttpServletRequest http, @PathVariable UUID accessId) { return service.approveAccess(principal.getName(), accessId, http); }
  @PostMapping("/access-requests/{accessId}/revoke")
  AccessResponse revoke(Principal principal, HttpServletRequest http, @PathVariable UUID accessId) { return service.revokeAccess(principal.getName(), accessId, http); }
}

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('HEALTH_ADMIN')")
class ClinicalAdminController {
  private final ClinicalService service;
  ClinicalAdminController(ClinicalService service) { this.service = service; }
  @GetMapping("/professionals/pending")
  List<ProfessionalResponse> pendingProfessionals() { return service.pendingProfessionals(); }
  @PostMapping("/professionals/{professionalProfileId}/approve")
  ProfessionalResponse approve(Principal principal, HttpServletRequest http, @PathVariable UUID professionalProfileId) { return service.setProfessionalStatus(principal.getName(), professionalProfileId, ProfessionalVerificationStatus.APPROVED, http); }
  @PostMapping("/professionals/{professionalProfileId}/reject")
  ProfessionalResponse reject(Principal principal, HttpServletRequest http, @PathVariable UUID professionalProfileId) { return service.setProfessionalStatus(principal.getName(), professionalProfileId, ProfessionalVerificationStatus.REJECTED, http); }
  @PostMapping("/professionals/{professionalProfileId}/suspend")
  ProfessionalResponse suspend(Principal principal, HttpServletRequest http, @PathVariable UUID professionalProfileId) { return service.setProfessionalStatus(principal.getName(), professionalProfileId, ProfessionalVerificationStatus.SUSPENDED, http); }
  @GetMapping("/medical-catalog")
  List<CatalogResponse> catalog() { return service.catalog(); }
  @PostMapping("/medical-catalog")
  CatalogResponse createCatalog(Principal principal, @Valid @RequestBody CatalogRequest request) { return service.createCatalog(principal.getName(), request); }
  @PostMapping("/exam-catalog")
  ExamCatalogResponse createExam(@Valid @RequestBody ExamCatalogRequest request) { return service.createExamCatalog(request); }
  @GetMapping("/clinical-protocols")
  List<String> protocols() { return List.of(); }
}
