package org.dvlang.compiler.ast;

import java.util.List;

/** One parsed {@code .dv} file. */
public record CompilationUnit(String fileUri, List<TopLevelDecl> declarations, SourceRange range)
        implements AstNode {
}
