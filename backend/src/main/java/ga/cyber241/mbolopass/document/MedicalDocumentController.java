package ga.cyber241.mbolopass.document;

import ga.cyber241.mbolopass.document.MedicalDocumentService.DocumentResponse;
import ga.cyber241.mbolopass.document.MedicalDocumentService.DownloadFile;
import ga.cyber241.mbolopass.document.MedicalDocumentService.UpdateRequest;
import ga.cyber241.mbolopass.document.MedicalDocumentService.UploadRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MedicalDocumentController {
  private final MedicalDocumentService service;

  public MedicalDocumentController(MedicalDocumentService service) { this.service = service; }

  @GetMapping("/api/v1/documents")
  List<DocumentResponse> documents(Principal principal) { return service.listPatient(principal.getName()); }

  @PostMapping(path = "/api/v1/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  DocumentResponse upload(Principal principal, @RequestPart("file") MultipartFile file, @Valid @RequestPart("metadata") UploadRequest metadata) {
    return service.uploadPatient(principal.getName(), file, metadata);
  }

  @GetMapping("/api/v1/documents/{documentId}/download")
  ResponseEntity<byte[]> download(Principal principal, @PathVariable UUID documentId) {
    DownloadFile file = service.download(principal.getName(), documentId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
        .body(file.bytes());
  }

  @PatchMapping("/api/v1/documents/{documentId}")
  DocumentResponse update(Principal principal, @PathVariable UUID documentId, @RequestBody UpdateRequest request) { return service.update(principal.getName(), documentId, request); }

  @DeleteMapping("/api/v1/documents/{documentId}")
  void archive(Principal principal, @PathVariable UUID documentId) { service.archive(principal.getName(), documentId); }

  @GetMapping("/api/v1/dependents/{dependentId}/documents")
  List<DocumentResponse> dependentDocuments(Principal principal, @PathVariable UUID dependentId) { return service.listDependent(principal.getName(), dependentId); }

  @PostMapping(path = "/api/v1/dependents/{dependentId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  DocumentResponse uploadDependent(Principal principal, @PathVariable UUID dependentId, @RequestPart("file") MultipartFile file, @Valid @RequestPart("metadata") UploadRequest metadata) {
    return service.uploadDependent(principal.getName(), dependentId, file, metadata);
  }
}
