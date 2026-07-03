package ga.cyber241.mbolopass.admin.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {}