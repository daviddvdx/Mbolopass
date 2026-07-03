package ga.cyber241.mbolopass.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {
  List<MedicalDocument> findByHealthProfileIdAndArchivedFalse(UUID healthProfileId);
  List<MedicalDocument> findByDependentProfileIdAndArchivedFalse(UUID dependentProfileId);
  long countByArchivedFalse();
}
