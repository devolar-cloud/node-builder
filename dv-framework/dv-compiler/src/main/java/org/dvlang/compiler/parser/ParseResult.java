package org.dvlang.compiler.parser;

import java.util.List;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.diagnostics.Diagnostic;

/** Result of parsing one file: a (possibly partial) model plus any parse diagnostics. */
public record ParseResult(CompilationUnit unit, List<Diagnostic> diagnostics) {
}
