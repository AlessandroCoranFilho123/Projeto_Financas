package app.smoke;

import app.database.Database;
import app.support.FxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Smoke - views de planejamento")
class PlanejamentoViewsSmokeTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initJavaFx() {
        FxTestSupport.initToolkit();
    }

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("carrega view de cartoes")
    void carregaViewDeCartoes() {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve("cartoes-view.db").toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/cartoes_view.fxml"));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);

        assertAll(
                () -> assertNotNull(root.lookup(".view-title")),
                () -> assertNotNull((Button) loader.getNamespace().get("btnNova")),
                () -> assertNotNull((FlowPane) loader.getNamespace().get("flowCartoes"))
        );
    }

    @Test
    @DisplayName("carrega view de emprestimos")
    void carregaViewDeEmprestimos() {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve("emprestimos-view.db").toAbsolutePath());
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/emprestimos_view.fxml"));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);

        assertAll(
                () -> assertNotNull(root.lookup(".view-title")),
                () -> assertNotNull((Button) loader.getNamespace().get("btnNovo")),
                () -> assertNotNull((VBox) loader.getNamespace().get("boxEmprestei")),
                () -> assertNotNull((VBox) loader.getNamespace().get("boxPedi"))
        );
    }
}
