package org.dvlang.compiler;

import java.util.List;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.diagnostics.Diagnostic;
import org.dvlang.compiler.symbols.SymbolTable;

/**
 * Result of {@link Workspace#update(String, String)}: the (possibly partial) model
 * for the updated file, its diagnostics (parse and semantic, combined), and the
 * workspace symbol table used to resolve it (brief section 3, compiler core
 * requirement 6).
 */
public record CompilationResult(CompilationUnit model, List<Diagnostic> diagnostics, SymbolTable symbols) {
}
