package org.dvlang.compiler.ast;

/**
 * A half-open source span: {@code [startLine:startColumn, endLine:endColumn]}, both
 * 1-based lines and 0-based columns to match ANTLR's {@code Token} convention.
 * Every AST node carries one (brief section 3, compiler core requirement 2).
 */
public record SourceRange(String fileUri, int startLine, int startColumn, int endLine, int endColumn) {

    public static SourceRange of(String fileUri, int line, int column) {
        return new SourceRange(fileUri, line, column, line, column);
    }
}
