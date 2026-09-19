package org.dvlang.compiler.ast;

/**
 * {@code @JS { ... }}. The body is opaque in v1 (not parsed): {@code rawBody} is the
 * exact source text between (not including) the outer braces.
 */
public record JsBlockDecl(String rawBody, SourceRange range) implements TopLevelDecl {
}
