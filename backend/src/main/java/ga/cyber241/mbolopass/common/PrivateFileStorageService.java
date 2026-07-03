package ga.cyber241.mbolopass.common;

import ga.cyber241.mbolopass.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PrivateFileStorageService {
  public static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
  public static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
  public static final int MAX_IMAGE_WIDTH = 4096;
  public static final int MAX_IMAGE_HEIGHT = 4096;
  private static final Set<String> PHOTO_MIMES = Set.of("image/jpeg", "image/png", "image/webp");
  private static final Map<String, String> PHOTO_EXTENSIONS = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
  private static final Set<String> DOCUMENT_MIMES = Set.of("application/pdf", "image/jpeg", "image/png");
  private static final Map<String, String> DOCUMENT_EXTENSIONS = Map.of("application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");
  private final Path root = Path.of("storage", "uploads").toAbsolutePath().normalize();

  public StoredFile savePhoto(MultipartFile file, String prefix) {
    return save(file, prefix, MAX_PHOTO_BYTES, PHOTO_MIMES, PHOTO_EXTENSIONS, true);
  }

  public StoredFile saveDocument(MultipartFile file, String prefix) {
    return save(file, prefix, MAX_DOCUMENT_BYTES, DOCUMENT_MIMES, DOCUMENT_EXTENSIONS, false);
  }

  public byte[] read(String storageKey) {
    try {
      Path path = resolve(storageKey);
      if (!Files.exists(path)) throw new ApiException(HttpStatus.NOT_FOUND, "Fichier introuvable");
      return Files.readAllBytes(path);
    } catch (IOException ex) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lecture fichier impossible");
    }
  }

  public void deleteQuietly(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) return;
    try { Files.deleteIfExists(resolve(storageKey)); } catch (IOException ignored) {}
  }

  private StoredFile save(MultipartFile file, String prefix, long maxBytes, Set<String> allowedMimes, Map<String, String> extensions, boolean image) {
    if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Fichier requis");
    if (file.getSize() > maxBytes) throw new ApiException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux");
    String mime = file.getContentType();
    if (mime == null || !allowedMimes.contains(mime)) throw new ApiException(HttpStatus.BAD_REQUEST, "Type de fichier refuse");
    String key = prefix + "/" + UUID.randomUUID() + extensions.get(mime);
    Path path = resolve(key);
    try {
      byte[] bytes = file.getBytes();
      byte[] storedBytes = image ? sanitizeImage(bytes, mime) : bytes;
      Files.createDirectories(path.getParent());
      Files.write(path, storedBytes);
      return new StoredFile(key, safeName(file.getOriginalFilename()), mime, storedBytes.length);
    } catch (IOException ex) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Stockage fichier impossible");
    }
  }

  private byte[] sanitizeImage(byte[] bytes, String mime) throws IOException {
    return switch (mime) {
      case "image/jpeg" -> sanitizeRaster(bytes, "jpg");
      case "image/png" -> sanitizeRaster(bytes, "png");
      case "image/webp" -> validateWebp(bytes);
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type de fichier refuse");
    };
  }

  private byte[] sanitizeRaster(byte[] bytes, String format) throws IOException {
    if (bytes.length < 8) throw new ApiException(HttpStatus.BAD_REQUEST, "Image invalide");
    if ("jpg".equals(format) && !(u8(bytes, 0) == 0xFF && u8(bytes, 1) == 0xD8 && u8(bytes, bytes.length - 2) == 0xFF && u8(bytes, bytes.length - 1) == 0xD9)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Signature image invalide");
    }
    if ("png".equals(format) && !(u8(bytes, 0) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Signature image invalide");
    }
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
    if (image == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Image invalide");
    validateDimensions(image.getWidth(), image.getHeight());
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(image, format, output)) throw new ApiException(HttpStatus.BAD_REQUEST, "Image invalide");
    return output.toByteArray();
  }

  private byte[] validateWebp(byte[] bytes) {
    if (bytes.length < 30 || bytes[0] != 0x52 || bytes[1] != 0x49 || bytes[2] != 0x46 || bytes[3] != 0x46 || bytes[8] != 0x57 || bytes[9] != 0x45 || bytes[10] != 0x42 || bytes[11] != 0x50) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Signature image invalide");
    }
    int[] dimensions = webpDimensions(bytes);
    validateDimensions(dimensions[0], dimensions[1]);
    return bytes;
  }

  private int[] webpDimensions(byte[] bytes) {
    if (bytes[12] == 0x56 && bytes[13] == 0x50 && bytes[14] == 0x38 && bytes[15] == 0x58 && bytes.length >= 30) {
      int width = 1 + little24(bytes, 24);
      int height = 1 + little24(bytes, 27);
      return new int[] { width, height };
    }
    if (bytes[12] == 0x56 && bytes[13] == 0x50 && bytes[14] == 0x38 && bytes[15] == 0x4C && bytes.length >= 25) {
      int b0 = u8(bytes, 21);
      int b1 = u8(bytes, 22);
      int b2 = u8(bytes, 23);
      int b3 = u8(bytes, 24);
      int width = 1 + (((b1 & 0x3F) << 8) | b0);
      int height = 1 + (((b3 & 0x0F) << 10) | (b2 << 2) | ((b1 & 0xC0) >> 6));
      return new int[] { width, height };
    }
    if (bytes[12] == 0x56 && bytes[13] == 0x50 && bytes[14] == 0x38 && bytes[15] == 0x20 && bytes.length >= 30) {
      int width = u8(bytes, 26) | ((u8(bytes, 27) & 0x3F) << 8);
      int height = u8(bytes, 28) | ((u8(bytes, 29) & 0x3F) << 8);
      return new int[] { width, height };
    }
    throw new ApiException(HttpStatus.BAD_REQUEST, "Image WebP invalide");
  }

  private void validateDimensions(int width, int height) {
    if (width <= 0 || height <= 0 || width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Dimensions image refusees");
    }
  }

  private int u8(byte[] bytes, int index) { return bytes[index] & 0xFF; }
  private int little24(byte[] bytes, int index) { return u8(bytes, index) | (u8(bytes, index + 1) << 8) | (u8(bytes, index + 2) << 16); }

  private Path resolve(String storageKey) {
    Path path = root.resolve(storageKey).normalize();
    if (!path.startsWith(root)) throw new ApiException(HttpStatus.BAD_REQUEST, "Chemin fichier invalide");
    return path;
  }

  private String safeName(String original) {
    String cleaned = StringUtils.cleanPath(original == null ? "document" : original);
    return cleaned.replace("\\", "_").replace("/", "_");
  }

  public record StoredFile(String storageKey, String originalFilename, String mimeType, long sizeBytes) {}
}
