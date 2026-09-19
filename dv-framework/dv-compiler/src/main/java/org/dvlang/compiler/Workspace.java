package org.dvlang.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.diagnostics.Diagnostic;
import org.dvlang.compiler.parser.DvParserFacade;
import org.dvlang.compiler.parser.ParseResult;
import org.dvlang.compiler.semantic.SemanticAnalyzer;
import org.dvlang.compiler.symbols.SymbolTable;

/**
 * Public entry point for both the LSP server and the runtime (brief section 3,
 * compiler core requirement 6). Holds every known file's model so cross-file
 * references (a domain used by a {@code @Database} block, or a query referenced by
 * {@code onSubmit}, declared in a different file) resolve regardless of which file
 * changed last.
 *
 * <p>Whole-file reparse per change is fine; files are small (brief section 3,
 * compiler core requirement 6). {@link #update} only returns diagnostics for the
 * file it was called with; call it again for another file to get that file's
 * diagnostics against the same up-to-date workspace symbol table.
 */
public final class Workspace {

    private final Map<String, CompilationUnit> units = new ConcurrentHashMap<>();

    public synchronized CompilationResult update(String uri, String text) {
        ParseResult parseResult = DvParserFacade.parse(uri, text);
        units.put(uri, parseResult.unit());

        SymbolTable symbols = SymbolTable.build(units);
        List<Diagnostic> semanticDiagnostics = new SemanticAnalyzer(symbols).analyze(uri, parseResult.unit());

        List<Diagnostic> all = new ArrayList<>(parseResult.diagnostics());
        all.addAll(semanticDiagnostics);
        return new CompilationResult(parseResult.unit(), List.copyOf(all), symbols);
    }

    public Optional<CompilationUnit> unit(String uri) {
        return Optional.ofNullable(units.get(uri));
    }
}
