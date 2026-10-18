import com.codivis.crm.CodivisApplication;
import java.nio.file.Files;
import java.nio.file.Path;

/** Inicio sencillo mediante Run Java en VS Code. */
public class Run {
    public static void main(String[] args) {
        Path proyecto = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(proyecto.resolve("pom.xml"))
                && Files.isRegularFile(proyecto.resolve("proyecto-aa5-ev01/pom.xml"))) {
            proyecto = proyecto.resolve("proyecto-aa5-ev01");
        }
        System.setProperty("crm.data.dir", proyecto.resolve("data").toString());
        System.out.println("Abre http://localhost:8086/crm/ cuando termine de iniciar.");
        CodivisApplication.main(args);
    }
}
