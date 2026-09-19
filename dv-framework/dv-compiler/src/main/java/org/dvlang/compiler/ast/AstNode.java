package org.dvlang.compiler.ast;

/**
 * Root of the AST sealed hierarchy. Every node is an immutable record and carries a
 * {@link SourceRange} (brief section 3, compiler core requirements 2 and 3).
 */
public sealed interface AstNode
        permits CompilationUnit, TopLevelDecl, FieldDecl, AnnotationNode, AnnotationArg,
        QueryDecl, ComponentMember, FieldRef {

    SourceRange range();
}
