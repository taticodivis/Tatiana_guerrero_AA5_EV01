package com.codivis.crm;

import com.codivis.crm.auth.UsuarioRepository;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Spring Boot inicia el servidor integrado y configura los controladores HTTP. */
@SpringBootApplication
public class CodivisApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodivisApplication.class, args);
    }

    @Bean
    UsuarioRepository usuarios(@Value("${crm.data.dir}") String carpeta) throws IOException {
        // Mantiene el archivo de cuentas utilizado antes de migrar a Spring Boot.
        return new UsuarioRepository(Path.of(carpeta, "usuario.properties"));
    }
}
