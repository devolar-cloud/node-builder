package org.dvlang.compiler.parser;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.dvlang.compiler.ast.SourceRange;
import org.dvlang.compiler.diagnostics.Diagnostic;
import org.dvlang.compiler.diagnostics.DiagnosticCodes;

/**
 * Collects ANTLR syntax errors as {@link Diagnostic}s instead of letting them abort
 * the parse. Combined with ANTLR's default error recovery (left on, never replaced
 * with a bail strategy) this is what lets a half-typed file still return a partial
 * AST plus diagnostics (brief section 3, compiler core requirement 1).
 */
final class CollectingErrorListener extends BaseErrorListener {

    private final String fileUri;
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    CollectingErrorListener(String fileUri) {
        this.fileUri = fileUri;
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
        int length = 1;
        if (offendingSymbol instanceof Token token && token.getText() != null && !token.getText().isEmpty()) {
            length = token.getText().length();
        }
        SourceRange range = new SourceRange(fileUri, line, charPositionInLine, line, charPositionInLine + length);
        diagnostics.add(Diagnostic.error(DiagnosticCodes.PARSE_ERROR, msg, range));
    }

    List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
