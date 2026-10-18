package com.codivis.crm.auth;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Cuentas locales: usuario único y contraseña almacenada como hash con sal. */
public final class UsuarioRepository {
    private static final int ITERACIONES = 600_000;
    private final Path archivo;
    private Map<String, Cuenta> usuarios = new LinkedHashMap<>();
    private record Cuenta(byte[] sal, byte[] hash) { }

    public UsuarioRepository(Path archivo) throws IOException {
        this.archivo = archivo.toAbsolutePath();
        if (!Files.exists(archivo)) return;
        Properties datos = new Properties();
        datos.load(new StringReader(Files.readString(archivo, StandardCharsets.UTF_8)));
        try {
            if (datos.getProperty("nombre") != null) {
                // Conserva una cuenta creada con el formato inicial.
                usuarios.put(datos.getProperty("nombre"), cuenta(datos.getProperty("sal", ""), datos.getProperty("hash", "")));
            } else {
                for (String clave : datos.stringPropertyNames()) {
                    if (!clave.startsWith("usuario.") || !clave.endsWith(".hash")) continue;
                    String nombre = clave.substring(8, clave.length() - 5);
                    if (!nombre.matches("[a-z0-9._-]{3,40}")) throw new IllegalArgumentException();
                    usuarios.put(nombre, cuenta(datos.getProperty("usuario." + nombre + ".sal", ""), datos.getProperty(clave)));
                }
            }
            if (usuarios.isEmpty()) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IOException("El archivo de usuarios no es válido.", ex);
        }
    }

    public synchronized boolean configurado() {
        return !usuarios.isEmpty();
    }

    public synchronized String nombre() {
        return usuarios.keySet().stream().findFirst().orElse(null);
    }

    public synchronized void crear(String nombre, String clave, String confirmacion) throws IOException {
        nombre = normalizar(nombre);
        if (!nombre.matches("[a-z0-9._-]{3,40}")) {
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 40 caracteres: letras, números, punto, guion o guion bajo.");
        }
        if (usuarios.containsKey(nombre)) throw new IllegalArgumentException("Ese usuario ya está registrado.");
        if (clave == null || clave.length() < 12 || clave.length() > 128) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");
        }
        if (!clave.equals(confirmacion)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        Map<String, Cuenta> nuevos = new LinkedHashMap<>(usuarios);
        nuevos.put(nombre, new Cuenta(sal, derivar(clave, sal)));
        Properties datos = new Properties();
        nuevos.forEach((usuario, cuenta) -> {
            datos.setProperty("usuario." + usuario + ".sal", Base64.getEncoder().encodeToString(cuenta.sal()));
            datos.setProperty("usuario." + usuario + ".hash", Base64.getEncoder().encodeToString(cuenta.hash()));
        });
        StringWriter contenido = new StringWriter();
        datos.store(contenido, "Cuentas Codivis - PBKDF2 SHA256, 600000 iteraciones");
        Files.createDirectories(archivo.getParent());
        Path temporal = Files.createTempFile(archivo.getParent(), "usuarios-", ".tmp");
        try {
            Files.writeString(temporal, contenido.toString(), StandardCharsets.UTF_8);
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            usuarios = nuevos;
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public synchronized boolean autenticar(String nombre, String clave) {
        if (clave == null || clave.length() > 128) return false;
        Cuenta cuenta = usuarios.get(normalizar(nombre));
        byte[] calculado = derivar(clave, cuenta == null ? new byte[16] : cuenta.sal());
        return cuenta != null && MessageDigest.isEqual(calculado, cuenta.hash());
    }

    private static Cuenta cuenta(String sal, String hash) {
        byte[] salBytes = Base64.getDecoder().decode(sal);
        byte[] hashBytes = Base64.getDecoder().decode(hash);
        if (salBytes.length != 16 || hashBytes.length != 32) throw new IllegalArgumentException();
        return new Cuenta(salBytes, hashBytes);
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
    }

    private static byte[] derivar(String clave, byte[] sal) {
        char[] caracteres = clave.toCharArray();
        PBEKeySpec especificacion = new PBEKeySpec(caracteres, sal, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No está disponible el algoritmo de contraseñas.", ex);
        } finally {
            especificacion.clearPassword();
            Arrays.fill(caracteres, '\0');
        }
    }
}
