package ga.cyber241.mbolopass.user;

import ga.cyber241.mbolopass.common.Enums.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
  long countByEnabledTrue();
  long countByRole(Role role);
  long countByRoleAndEnabledTrue(Role role);
}
