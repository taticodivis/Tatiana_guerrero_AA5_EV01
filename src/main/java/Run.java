import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.ArrayList;

/** Run Java inicia Tomcat dentro de esta JVM; detener Run tambien detiene el servidor. */
public class Run {
    public static void main(String[] args) throws Exception {
        Path proyecto = encontrarProyecto();
        System.out.println("Preparando el proyecto...");
        Process preparacion = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", proyecto.resolve("Iniciar.ps1").toString(), "-SoloPreparar")
                .directory(proyecto.toFile()).inheritIO().start();
        if (preparacion.waitFor() != 0) {
            System.err.println("El proyecto no inicio. Revisa el mensaje anterior.");
            return;
        }

        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(proyecto.resolve("target/runner.properties"), StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        Path tomcat = Path.of(config.getProperty("tomcat.home"));
        System.setProperty("catalina.home", tomcat.toString());
        System.setProperty("catalina.base", config.getProperty("tomcat.base"));
        System.setProperty("java.io.tmpdir", Path.of(config.getProperty("tomcat.base"), "temp").toString());
        System.setProperty("crm.data.dir", config.getProperty("data.dir"));

        ArrayList<URL> jars = new ArrayList<>();
        jars.add(tomcat.resolve("bin/bootstrap.jar").toUri().toURL());
        jars.add(tomcat.resolve("bin/tomcat-juli.jar").toUri().toURL());
        try (var archivos = Files.newDirectoryStream(tomcat.resolve("lib"), "*.jar")) {
            for (Path archivo : archivos) jars.add(archivo.toUri().toURL());
        }
        // Aisla las bibliotecas del servidor del classpath que usa VS Code.
        try (URLClassLoader loader = new URLClassLoader(jars.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> catalina = loader.loadClass("org.apache.catalina.startup.Catalina");
            Object servidor = catalina.getConstructor().newInstance();
            catalina.getMethod("setParentClassLoader", ClassLoader.class).invoke(servidor, loader);
            catalina.getMethod("setUseShutdownHook", boolean.class).invoke(servidor, false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    catalina.getMethod("stop").invoke(servidor);
                } catch (Exception ex) {
                    System.err.println("El servidor esta cerrandose.");
                }
            }, "CRM-detener"));
            catalina.getMethod("setAwait", boolean.class).invoke(servidor, true);
            catalina.getMethod("load").invoke(servidor);
            System.out.println("Abre http://localhost:" + config.getProperty("port") + "/crm/");
            System.out.println("Detener esta ejecucion de Run Java cierra el servidor.");
            catalina.getMethod("start").invoke(servidor);
        }
    }

    private static Path encontrarProyecto() {
        Path carpeta = Path.of("").toAbsolutePath();
        while (carpeta != null) {
            if (Files.isRegularFile(carpeta.resolve("Iniciar.ps1"))) return carpeta;
            Path proyecto = carpeta.resolve("proyecto-aa5-ev01");
            if (Files.isRegularFile(proyecto.resolve("Iniciar.ps1"))) return proyecto;
            carpeta = carpeta.getParent();
        }
        throw new IllegalStateException("Abre la carpeta completa del proyecto en VS Code.");
    }
}
