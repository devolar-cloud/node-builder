package org.dvlang.compiler.diagnostics;

import org.dvlang.compiler.ast.SourceRange;

/**
 * One diagnostic with severity, code, message and range (brief section 3, compiler
 * core requirement 4). Semantic analysis and the parser both produce these; neither
 * throws for a user error.
 */
public record Diagnostic(Severity severity, String code, String message, SourceRange range) {

    public static Diagnostic error(String code, String message, SourceRange range) {
        return new Diagnostic(Severity.ERROR, code, message, range);
    }
}
