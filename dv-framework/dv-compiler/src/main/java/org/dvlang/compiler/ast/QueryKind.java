package org.dvlang.compiler.ast;

/** Result shape of a {@link QueryDecl} (grammar rule {@code queryKind}). */
public enum QueryKind {
    VERIFY,
    EXISTS,
    ONE,
    LIST
}
