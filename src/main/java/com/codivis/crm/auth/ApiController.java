package com.codivis.crm.auth;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Contrato del caso: registro y autenticacion con respuestas JSON, sin sesiones. */
@RestController
@RequestMapping("/api")
public class ApiController {
    private final UsuarioRepository usuarios;
    public record Resultado(boolean ok, String codigo, String mensaje) { }

    public ApiController(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @GetMapping("/estado")
    public ResponseEntity<Resultado> estado() {
        return respuesta(200, true, "API_DISPONIBLE", "API Codivis disponible.");
    }

    @PostMapping(value = "/registro", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Resultado> registro(@RequestParam(required = false) String usuario,
            @RequestParam(required = false) String clave, @RequestParam(required = false) String confirmacion)
            throws IOException {
        if (vacio(usuario) || vacio(clave)) return campos();
        try {
            usuarios.crear(usuario, clave, confirmacion);
            return respuesta(201, true, "USUARIO_CREADO", "Usuario registrado correctamente.");
        } catch (IllegalArgumentException ex) {
            boolean duplicado = "Ese usuario ya está registrado.".equals(ex.getMessage());
            return respuesta(duplicado ? 409 : 400, false,
                    duplicado ? "USUARIO_DUPLICADO" : "DATOS_INVALIDOS", ex.getMessage());
        }
    }

    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Resultado> login(@RequestParam(required = false) String usuario,
            @RequestParam(required = false) String clave) {
        if (vacio(usuario) || vacio(clave)) return campos();
        boolean correcto = usuarios.autenticar(usuario, clave);
        return respuesta(correcto ? 200 : 401, correcto,
                correcto ? "AUTENTICACION_CORRECTA" : "CREDENCIALES_INVALIDAS",
                correcto ? "Autenticacion satisfactoria." : "Usuario o clave incorrectos.");
    }

    private static boolean vacio(String texto) { return texto == null || texto.isBlank(); }
    private static ResponseEntity<Resultado> campos() {
        return respuesta(400, false, "CAMPOS_OBLIGATORIOS", "Usuario y clave son obligatorios.");
    }
    static ResponseEntity<Resultado> respuesta(int estado, boolean ok, String codigo, String mensaje) {
        return ResponseEntity.status(estado).header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff").body(new Resultado(ok, codigo, mensaje));
    }
}
