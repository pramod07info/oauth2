import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.util.*;

public class GenerateKey {
  static String b(BigInteger n) {
    byte[] a = n.toByteArray();
    if (a[0] == 0) a = Arrays.copyOfRange(a, 1, a.length);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(a);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1)
      throw new IllegalArgumentException("Usage: java scripts/GenerateKey.java secrets/jwk.json");
    var path = Path.of(args[0]);
    if (Files.exists(path)) throw new IllegalArgumentException("Refusing to replace existing key");
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(3072);
    var pair = generator.generateKeyPair();
    var k = (RSAPrivateCrtKey) pair.getPrivate();
    var json =
        String.format(
            "{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"%s\",\"n\":\"%s\",\"e\":\"%s\",\"d\":\"%s\",\"p\":\"%s\",\"q\":\"%s\",\"dp\":\"%s\",\"dq\":\"%s\",\"qi\":\"%s\"}",
            UUID.randomUUID(),
            b(k.getModulus()),
            b(k.getPublicExponent()),
            b(k.getPrivateExponent()),
            b(k.getPrimeP()),
            b(k.getPrimeQ()),
            b(k.getPrimeExponentP()),
            b(k.getPrimeExponentQ()),
            b(k.getCrtCoefficient()));
    if (path.getParent() != null) Files.createDirectories(path.getParent());
    Files.writeString(path, json, StandardOpenOption.CREATE_NEW);
    try {
      Files.setPosixFilePermissions(
          path, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    } catch (UnsupportedOperationException ignored) {
    }
    System.out.println("Created RSA signing key: " + path);
  }
}
