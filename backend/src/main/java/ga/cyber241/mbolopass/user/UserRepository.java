package ga.cyber241.mbolopass.user;

import ga.cyber241.mbolopass.common.Enums.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
  List<User> findByRole(Role role);
  @Query("""
      select u from User u
      where u.role = :role and u.enabled = true and (
        :query = '' or
        lower(u.email) like lower(concat('%', :query, '%')) or
        lower(u.firstName) like lower(concat('%', :query, '%')) or
        lower(u.lastName) like lower(concat('%', :query, '%'))
      )
      """)
  Page<User> searchEnabledByRole(@Param("role") Role role, @Param("query") String query, Pageable pageable);
  long countByEnabledTrue();
  long countByRole(Role role);
  long countByRoleAndEnabledTrue(Role role);
}
