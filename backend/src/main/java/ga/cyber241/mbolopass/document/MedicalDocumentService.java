package ga.cyber241.mbolopass.document;

import ga.cyber241.mbolopass.common.Enums.DocumentOwnerType;
import ga.cyber241.mbolopass.common.Enums.MedicalDocumentCategory;
import ga.cyber241.mbolopass.common.PrivateFileStorageService;
import ga.cyber241.mbolopass.common.PrivateFileStorageService.StoredFile;
import ga.cyber241.mbolopass.dependent.DependentProfile;
import ga.cyber241.mbolopass.dependent.DependentService;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MedicalDocumentService {
  private final MedicalDocumentRepository documents;
  private final HealthProfileService healthProfiles;
  private final DependentService dependents;
  private final UserRepository users;
  private final PrivateFileStorageService storage;

  public MedicalDocumentService(MedicalDocumentRepository documents, HealthProfileService healthProfiles, DependentService dependents, UserRepository users, PrivateFileStorageService storage) {
    this.documents = documents;
    this.healthProfiles = healthProfiles;
    this.dependents = dependents;
    this.users = users;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listPatient(String email) {
    HealthProfile profile = healthProfiles.current(email);
    return documents.findByHealthProfileIdAndArchivedFalse(profile.getId()).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDependent(String email, UUID dependentId) {
    DependentProfile dependent = dependents.owned(email, dependentId);
    return documents.findByDependentProfileIdAndArchivedFalse(dependent.getId()).stream().map(this::toResponse).toList();
  }

  @Transactional
  public DocumentResponse uploadPatient(String email, MultipartFile file, UploadRequest request) {
    HealthProfile profile = healthProfiles.current(email);
    MedicalDocument document = createDocument(email, file, request);
    document.setOwnerType(DocumentOwnerType.PATIENT);
    document.setHealthProfile(profile);
    return toResponse(documents.save(document));
  }

  @Transactional
  public DocumentResponse uploadDependent(String email, UUID dependentId, MultipartFile file, UploadRequest request) {
    DependentProfile dependent = dependents.owned(email, dependentId);
    MedicalDocument document = createDocument(email, file, request);
    document.setOwnerType(DocumentOwnerType.DEPENDENT);
    document.setDependentProfile(dependent);
    return toResponse(documents.save(document));
  }

  @Transactional(readOnly = true)
  public DownloadFile download(String email, UUID documentId) {
    MedicalDocument document = owned(email, documentId);
    return new DownloadFile(storage.read(document.getStorageKey()), document.getMimeType(), document.getOriginalFilename());
  }

  @Transactional
  public DocumentResponse update(String email, UUID documentId, UpdateRequest request) {
    MedicalDocument document = owned(email, documentId);
    if (request.title() != null && !request.title().isBlank()) document.setTitle(request.title());
    if (request.category() != null) document.setCategory(request.category());
    document.setIssuedDate(request.issuedDate());
    return toResponse(documents.save(document));
  }

  @Transactional
  public void archive(String email, UUID documentId) {
    MedicalDocument document = owned(email, documentId);
    document.setArchived(true);
    documents.save(document);
  }

  public long countActive() { return documents.countByArchivedFalse(); }

  private MedicalDocument createDocument(String email, MultipartFile file, UploadRequest request) {
    StoredFile stored = storage.saveDocument(file, "medical-documents");
    MedicalDocument document = new MedicalDocument();
    document.setCategory(request.category() == null ? MedicalDocumentCategory.OTHER : request.category());
    document.setTitle(request.title());
    document.setOriginalFilename(stored.originalFilename());
    document.setStorageKey(stored.storageKey());
    document.setMimeType(stored.mimeType());
    document.setSizeBytes(stored.sizeBytes());
    document.setIssuedDate(request.issuedDate());
    document.setUploadedBy(actor(email));
    return document;
  }

  private MedicalDocument owned(String email, UUID documentId) {
    MedicalDocument document = documents.findById(documentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document introuvable"));
    if (document.isArchived()) throw new ApiException(HttpStatus.NOT_FOUND, "Document introuvable");
    if (document.getOwnerType() == DocumentOwnerType.PATIENT) {
      HealthProfile profile = healthProfiles.current(email);
      if (!document.getHealthProfile().getId().equals(profile.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse");
    } else {
      dependents.owned(email, document.getDependentProfile().getId());
    }
    return document;
  }

  private User actor(String email) {
    return users.findByEmail(email.toLowerCase()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
  }

  private DocumentResponse toResponse(MedicalDocument document) {
    return new DocumentResponse(document.getId(), document.getOwnerType().name(), document.getCategory().name(), document.getTitle(), document.getOriginalFilename(), document.getMimeType(), document.getSizeBytes(), document.getIssuedDate(), document.getUploadedAt());
  }

  public record UploadRequest(@NotBlank String title, MedicalDocumentCategory category, LocalDate issuedDate) {}
  public record UpdateRequest(String title, MedicalDocumentCategory category, LocalDate issuedDate) {}
  public record DocumentResponse(UUID id, String ownerType, String category, String title, String originalFilename, String mimeType, long sizeBytes, LocalDate issuedDate, Instant uploadedAt) {}
  public record DownloadFile(byte[] bytes, String mimeType, String filename) {}
}
