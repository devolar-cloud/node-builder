package org.dvlang.compiler.ast;

/** A declaration that can appear directly inside a {@link CompilationUnit}. */
public sealed interface TopLevelDecl extends AstNode
        permits DomainDecl, DatabaseDecl, ComponentDecl, JsBlockDecl, RawTemplateDecl {
}
