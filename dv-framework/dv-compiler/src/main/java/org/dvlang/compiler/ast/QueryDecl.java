package org.dvlang.compiler.ast;

import java.util.List;

/**
 * {@code query login(userName, password): verify}, {@code query existsByUserName(userName): exists}.
 */
public record QueryDecl(String name, List<String> params, QueryKind kind, SourceRange range) implements AstNode {
}
