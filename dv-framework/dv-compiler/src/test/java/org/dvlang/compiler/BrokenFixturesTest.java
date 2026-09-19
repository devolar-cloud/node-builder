package org.dvlang.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dvlang.compiler.diagnostics.Diagnostic;
import org.dvlang.compiler.diagnostics.DiagnosticCodes;
import org.dvlang.compiler.parser.DvParserFacade;
import org.dvlang.compiler.parser.ParseResult;
import org.dvlang.compiler.semantic.SemanticAnalyzer;
import org.dvlang.compiler.symbols.SymbolTable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * M1 acceptance check 2 (brief section 11): 20 broken fixtures each give the
 * expected diagnostic with the correct range, and no fixture throws (compiler core
 * requirement 1). Each fixture is a single self-contained {@code .dv} file
 * exercising exactly one of the eight v1 semantic checks or a parse error.
 *
 * <p>{@code 17}/{@code 18} (unknown UI kind) run the semantic analyzer directly with
 * {@link SemanticAnalyzer#DEFAULT_UI_KINDS} rather than through {@link Workspace},
 * because v1 has no "active component map" wired up yet (brief section 3): the
 * check exists and is tested here, but {@link Workspace#update} leaves it disabled
 * until milestone M4 wires a real component map in.
 */
class BrokenFixturesTest {

    private record FixtureCase(String file, String expectedCode, int expectedLine, boolean uiKindCheck) {
        FixtureCase(String file, String expectedCode, int expectedLine) {
            this(file, expectedCode, expectedLine, false);
        }
    }

    static Stream<Arguments> fixtures() {
        return Stream.of(
                new FixtureCase("01-unknown-annotation-1.dv", DiagnosticCodes.UNKNOWN_ANNOTATION, 3),
                new FixtureCase("02-unknown-annotation-2.dv", DiagnosticCodes.UNKNOWN_ANNOTATION, 3),
                new FixtureCase("03-unknown-validator-1.dv", DiagnosticCodes.UNKNOWN_VALIDATOR, 3),
                new FixtureCase("04-unknown-validator-2.dv", DiagnosticCodes.UNKNOWN_VALIDATOR, 3),
                new FixtureCase("05-bad-validator-argument-1.dv", DiagnosticCodes.BAD_VALIDATOR_ARGUMENT, 3),
                new FixtureCase("06-bad-validator-argument-2.dv", DiagnosticCodes.BAD_VALIDATOR_ARGUMENT, 3),
                new FixtureCase("07-duplicate-field.dv", DiagnosticCodes.DUPLICATE_DECLARATION, 4),
                new FixtureCase("08-duplicate-query.dv", DiagnosticCodes.DUPLICATE_DECLARATION, 8),
                new FixtureCase("09-duplicate-component-member.dv", DiagnosticCodes.DUPLICATE_DECLARATION, 4),
                new FixtureCase("10-unresolved-database-domain.dv", DiagnosticCodes.UNRESOLVED_REFERENCE, 1),
                new FixtureCase("11-unresolved-onsubmit-domain.dv", DiagnosticCodes.UNRESOLVED_REFERENCE, 3),
                new FixtureCase("12-unresolved-onsubmit-query.dv", DiagnosticCodes.UNRESOLVED_REFERENCE, 12),
                new FixtureCase("13-field-not-in-domain-1.dv", DiagnosticCodes.FIELD_NOT_IN_DOMAIN, 14),
                new FixtureCase("14-field-not-in-domain-2.dv", DiagnosticCodes.FIELD_NOT_IN_DOMAIN, 15),
                new FixtureCase("15-secret-field-in-query-1.dv", DiagnosticCodes.SECRET_FIELD_IN_QUERY, 8),
                new FixtureCase("16-secret-field-in-query-2.dv", DiagnosticCodes.SECRET_FIELD_IN_QUERY, 8),
                new FixtureCase("17-unknown-ui-kind-1.dv", DiagnosticCodes.UNKNOWN_UI_KIND, 3, true),
                new FixtureCase("18-unknown-ui-kind-2.dv", DiagnosticCodes.UNKNOWN_UI_KIND, 3, true),
                new FixtureCase("19-parse-error-missing-brace.dv", DiagnosticCodes.PARSE_ERROR, 4),
                new FixtureCase("20-parse-error-bad-token.dv", DiagnosticCodes.PARSE_ERROR, 3))
                .map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void fixtureYieldsExpectedDiagnostic(FixtureCase fixture) throws IOException {
        String uri = "file:///" + fixture.file();
        String source = Files.readString(fixturePath(fixture.file()));

        List<Diagnostic> diagnostics = assertDoesNotThrow(
                () -> fixture.uiKindCheck() ? analyzeWithUiKindMap(uri, source) : analyzeThroughWorkspace(uri, source),
                () -> "fixture must not throw: " + fixture.file());

        boolean matched = diagnostics.stream().anyMatch(
                d -> d.code().equals(fixture.expectedCode()) && d.range().startLine() == fixture.expectedLine());
        assertTrue(matched, () -> "expected " + fixture.expectedCode() + " at line " + fixture.expectedLine()
                + " in " + fixture.file() + ", got: " + diagnostics);
    }

    private static List<Diagnostic> analyzeThroughWorkspace(String uri, String source) {
        return new Workspace().update(uri, source).diagnostics();
    }

    private static List<Diagnostic> analyzeWithUiKindMap(String uri, String source) {
        ParseResult parseResult = DvParserFacade.parse(uri, source);
        SymbolTable symbols = SymbolTable.build(Map.of(uri, parseResult.unit()));
        List<Diagnostic> diagnostics = new java.util.ArrayList<>(parseResult.diagnostics());
        diagnostics.addAll(new SemanticAnalyzer(symbols, SemanticAnalyzer.DEFAULT_UI_KINDS).analyze(uri, parseResult.unit()));
        return diagnostics;
    }

    private static Path fixturePath(String fileName) {
        return Path.of(System.getProperty("user.dir")).resolve("src/test/resources/fixtures").resolve(fileName);
    }
}
