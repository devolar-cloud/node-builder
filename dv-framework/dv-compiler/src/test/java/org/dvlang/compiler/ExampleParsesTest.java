package org.dvlang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.dvlang.compiler.ast.ComponentDecl;
import org.dvlang.compiler.ast.ComponentMember;
import org.dvlang.compiler.ast.DatabaseDecl;
import org.dvlang.compiler.ast.DomainDecl;
import org.dvlang.compiler.ast.JsBlockDecl;
import org.dvlang.compiler.ast.TopLevelDecl;
import org.junit.jupiter.api.Test;

/**
 * M1 acceptance check 1 (brief section 11): the example files parse to the model
 * with no diagnostics. {@code dv-example} is not a Maven module (brief section 2
 * module table: "none, consumed by tests"), so its files are read by relative path.
 */
class ExampleParsesTest {

    private static String read(String relativeToDvFramework) throws IOException {
        Path path = Path.of(System.getProperty("user.dir")).resolve(relativeToDvFramework);
        return Files.readString(path);
    }

    @Test
    void exampleFilesParseWithNoDiagnostics() throws IOException {
        Workspace workspace = new Workspace();

        CompilationResult userResult = workspace.update("file:///User.dv", read("../dv-example/User.dv"));
        CompilationResult loginResult = workspace.update("file:///UserLogin.dv", read("../dv-example/UserLogin.dv"));

        assertTrue(userResult.diagnostics().isEmpty(), () -> "User.dv diagnostics: " + userResult.diagnostics());
        assertTrue(loginResult.diagnostics().isEmpty(), () -> "UserLogin.dv diagnostics: " + loginResult.diagnostics());

        DomainDecl user = findDomain(userResult, "User");
        assertEquals(4, user.fields().size());
        assertTrue(user.hasField("userId"));
        assertTrue(user.hasField("userName"));
        assertTrue(user.hasField("firstName"));
        assertTrue(user.hasField("password"));
        assertTrue(user.fields().stream()
                .filter(f -> f.name().equals("password"))
                .findFirst()
                .orElseThrow()
                .hasAnnotation("secret"));

        DatabaseDecl database = findDatabase(userResult);
        assertEquals("User", database.domainName());
        assertEquals("users", database.tableName());
        assertTrue(database.hasCrud());
        assertEquals(2, database.queries().size());

        ComponentDecl login = findComponent(loginResult, "UserLogin");
        assertEquals("FormLogin", login.uiProp().orElseThrow().uiKind());
        assertEquals("Please login", login.members().stream()
                .filter(ComponentMember.TitleProp.class::isInstance)
                .map(ComponentMember.TitleProp.class::cast)
                .findFirst()
                .orElseThrow()
                .value());
        assertEquals(2, login.fieldsBlock().orElseThrow().fields().size());
        assertEquals("User", login.onSubmitProp().orElseThrow().targetName());
        assertEquals("login", login.onSubmitProp().orElseThrow().memberName());

        JsBlockDecl jsBlock = loginResult.model().declarations().stream()
                .filter(JsBlockDecl.class::isInstance)
                .map(JsBlockDecl.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(jsBlock.rawBody().contains("strongPassword"));
        assertFalse(jsBlock.rawBody().isBlank());
    }

    private static DomainDecl findDomain(CompilationResult result, String name) {
        for (TopLevelDecl decl : result.model().declarations()) {
            if (decl instanceof DomainDecl d && d.name().equals(name)) {
                return d;
            }
        }
        throw new AssertionError("domain not found: " + name);
    }

    private static DatabaseDecl findDatabase(CompilationResult result) {
        for (TopLevelDecl decl : result.model().declarations()) {
            if (decl instanceof DatabaseDecl d) {
                return d;
            }
        }
        throw new AssertionError("database not found");
    }

    private static ComponentDecl findComponent(CompilationResult result, String name) {
        for (TopLevelDecl decl : result.model().declarations()) {
            if (decl instanceof ComponentDecl c && c.name().equals(name)) {
                return c;
            }
        }
        throw new AssertionError("component not found: " + name);
    }
}
