package com.codivis.crm.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** API del caso AA5-EV01: registro y verificacion de credenciales, sin crear sesiones. */
@WebServlet(urlPatterns = {"/api/registro", "/api/login", "/api/estado"}, loadOnStartup = 1)
public class ApiServlet extends HttpServlet {
    private UsuarioRepository usuarios;

    @Override
    public void init() throws ServletException {
        Path directorio = Path.of(System.getProperty("crm.data.dir",
                Path.of(System.getProperty("catalina.base"), "data", "api").toString()));
        try {
            usuarios = new UsuarioRepository(directorio.resolve("usuario.properties"));
        } catch (IOException ex) {
            throw new ServletException("No se pudieron leer los usuarios.", ex);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json");
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        boolean estado = "/api/estado".equals(req.getServletPath());
        if (!(estado ? "GET" : "POST").equals(req.getMethod())) {
            res.setHeader("Allow", estado ? "GET" : "POST");
            responder(res, 405, false, "METODO_NO_PERMITIDO", "Metodo HTTP no permitido.");
            return;
        }
        super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        responder(res, 200, true, "API_DISPONIBLE", "API Codivis disponible.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Postman envia Body > x-www-form-urlencoded; la respuesta siempre es JSON.
        String tipo = req.getContentType();
        if (tipo == null || !tipo.split(";", 2)[0].strip().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            responder(res, 415, false, "FORMATO_NO_ADMITIDO", "Usa application/x-www-form-urlencoded.");
            return;
        }
        String usuario = req.getParameter("usuario");
        String clave = req.getParameter("clave");
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            responder(res, 400, false, "CAMPOS_OBLIGATORIOS", "Usuario y clave son obligatorios.");
            return;
        }
        try {
            if ("/api/registro".equals(req.getServletPath())) {
                // El repositorio sincroniza el alta y guarda hashes PBKDF2 con sal aleatoria.
                usuarios.crear(usuario, clave, req.getParameter("confirmacion"));
                responder(res, 201, true, "USUARIO_CREADO", "Usuario registrado correctamente.");
            } else if (usuarios.autenticar(usuario, clave)) {
                responder(res, 200, true, "AUTENTICACION_CORRECTA", "Autenticacion satisfactoria.");
            } else {
                responder(res, 401, false, "CREDENCIALES_INVALIDAS", "Usuario o clave incorrectos.");
            }
        } catch (IllegalArgumentException ex) {
            boolean duplicado = "Ese usuario ya esta registrado.".equals(sinAcentos(ex.getMessage()));
            responder(res, duplicado ? 409 : 400, false,
                    duplicado ? "USUARIO_DUPLICADO" : "DATOS_INVALIDOS", ex.getMessage());
        } catch (IOException ex) {
            getServletContext().log("Error guardando un usuario de la API", ex);
            responder(res, 500, false, "ERROR_INTERNO", "No se pudo guardar el usuario.");
        }
    }

    private static String sinAcentos(String valor) {
        return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static void responder(HttpServletResponse res, int estado, boolean ok, String codigo, String mensaje) throws IOException {
        res.setStatus(estado);
        res.getWriter().write("{\"ok\":" + ok + ",\"codigo\":\"" + codigo
                + "\",\"mensaje\":\"" + escapar(mensaje) + "\"}");
    }

    private static String escapar(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') resultado.append('\\').append(c);
            else if (c < 32) resultado.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else resultado.append(c);
        }
        return resultado.toString();
    }
}
