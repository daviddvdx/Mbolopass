package ga.cyber241.mbolopass.user;

import ga.cyber241.mbolopass.common.Enums.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter implements AttributeConverter<Role, String> {
  @Override
  public String convertToDatabaseColumn(Role role) {
    return role == null ? null : role.name();
  }

  @Override
  public Role convertToEntityAttribute(String value) {
    if (value == null || value.isBlank()) return Role.PATIENT;
    return switch (value) {
      case "PATIENT", "ROLE_PATIENT" -> Role.PATIENT;
      case "PROFESSIONAL", "ROLE_PROFESSIONAL", "HEALTH_PROFESSIONAL", "ROLE_HEALTH_PROFESSIONAL" -> Role.HEALTH_PROFESSIONAL;
      case "ADMIN", "ROLE_ADMIN", "HEALTH_ADMIN", "ROLE_HEALTH_ADMIN" -> Role.HEALTH_ADMIN;
      default -> Role.valueOf(value);
    };
  }
}
