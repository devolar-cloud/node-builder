package org.dvlang.compiler.parser;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.ast.SourceRange;
import org.dvlang.compiler.diagnostics.Diagnostic;
import org.dvlang.compiler.diagnostics.DiagnosticCodes;

/**
 * Public parsing entry point for {@code dv-compiler}. Parsing never throws (brief
 * section 3, compiler core requirement 1): ANTLR's default error recovery stays on,
 * syntax errors are collected as diagnostics instead of aborting the parse, and any
 * unexpected exception is still caught here as a last resort so a caller always gets
 * a {@link ParseResult} back.
 */
public final class DvParserFacade {

    private DvParserFacade() {
    }

    public static ParseResult parse(String fileUri, String source) {
        String text = source == null ? "" : source;
        List<Diagnostic> diagnostics = new ArrayList<>();
        CompilationUnit unit;
        try {
            CharStream input = CharStreams.fromString(text, fileUri);

            CollectingErrorListener listener = new CollectingErrorListener(fileUri);

            DvLexer lexer = new DvLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            DvParser parser = new DvParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(listener);
            // Default error strategy (resync and continue) stays on; see class javadoc.

            DvParser.CompilationUnitContext tree = parser.compilationUnit();
            unit = new AstBuilder(fileUri).build(tree);
            diagnostics.addAll(listener.diagnostics());
        } catch (RuntimeException unexpected) {
            unit = new CompilationUnit(fileUri, List.of(), SourceRange.of(fileUri, 1, 0));
            diagnostics.add(Diagnostic.error(
                    DiagnosticCodes.PARSE_ERROR,
                    "internal parser error: " + unexpected,
                    SourceRange.of(fileUri, 1, 0)));
        }
        return new ParseResult(unit, List.copyOf(diagnostics));
    }
}
