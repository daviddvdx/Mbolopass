package ga.cyber241.mbolopass.medical;

import ga.cyber241.mbolopass.medical.MedicalEnums.MedicalHistoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MedicalDtos {
  private MedicalDtos() {}

  public record PatientSearchPage(List<PatientSearchResultDto> content, int page, int size, long totalElements, int totalPages) {}
  public record PatientSearchResultDto(UUID id, String firstName, String lastName, String gender, Integer birthYear) {}

  public record MedicalRecordDto(
      UUID id,
      UUID patientId,
      String firstName,
      String lastName,
      String gender,
      Integer birthYear,
      List<MedicalHistoryEntryDto> history,
      List<MedicalConsultationDto> consultations) {}

  public record MedicalHistoryEntryDto(
      UUID id,
      String category,
      String title,
      String description,
      LocalDate startDate,
      LocalDate endDate,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {}

  public record CreateMedicalHistoryEntryRequest(
      @NotNull MedicalHistoryCategory category,
      @NotBlank @Size(min = 2, max = 140) String title,
      @Size(max = 2000) String description,
      @PastOrPresent LocalDate startDate,
      LocalDate endDate,
      Boolean active) {}

  public record UpdateMedicalHistoryEntryRequest(
      @NotNull MedicalHistoryCategory category,
      @NotBlank @Size(min = 2, max = 140) String title,
      @Size(max = 2000) String description,
      @PastOrPresent LocalDate startDate,
      LocalDate endDate,
      Boolean active) {}

  public record MedicalConsultationDto(
      UUID id,
      UUID healthProfessionalId,
      String professionalName,
      Instant consultationDate,
      String reason,
      String diagnosis,
      String notes,
      String treatment,
      LocalDate followUpDate,
      Instant createdAt,
      Instant updatedAt) {}

  public record CreateMedicalConsultationRequest(
      Instant consultationDate,
      @NotBlank @Size(min = 2, max = 500) String reason,
      @Size(max = 1200) String diagnosis,
      @Size(max = 3000) String notes,
      @Size(max = 1200) String treatment,
      LocalDate followUpDate) {}

  public record UpdateMedicalConsultationRequest(
      Instant consultationDate,
      @NotBlank @Size(min = 2, max = 500) String reason,
      @Size(max = 1200) String diagnosis,
      @Size(max = 3000) String notes,
      @Size(max = 1200) String treatment,
      LocalDate followUpDate) {}

  public record MedicalAccessRequestDto(
      UUID id,
      UUID patientId,
      UUID healthProfessionalId,
      String patientName,
      String professionalName,
      String status,
      String reason,
      Instant requestedAt,
      Instant respondedAt,
      Instant codeExpiresAt,
      MedicalAccessGrantDto activeGrant) {}

  public record CreateMedicalAccessRequest(
      @NotNull UUID patientId,
      @Size(max = 500) String reason) {}

  public record ActivateMedicalAccessRequest(
      @NotBlank @Size(min = 6, max = 8) String code) {}

  public record TemporaryAccessCodeResponse(String code, Instant expiresAt) {}
  public record MedicalAccessGrantDto(UUID id, UUID patientId, UUID healthProfessionalId, Instant activatedAt, Instant expiresAt, String status) {}
}
