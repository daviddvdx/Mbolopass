package ga.cyber241.mbolopass.common;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;

@MappedSuperclass
public abstract class UuidEntity {
  @Id
  private UUID id;

  @PrePersist
  public void ensureId() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
}
