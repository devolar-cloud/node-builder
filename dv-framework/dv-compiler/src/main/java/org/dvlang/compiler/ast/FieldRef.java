package org.dvlang.compiler.ast;

/** {@code userName} or {@code password (passwordField)} inside a {@code fields { ... }} block. */
public record FieldRef(String name, String widget, SourceRange range) implements AstNode {
}
