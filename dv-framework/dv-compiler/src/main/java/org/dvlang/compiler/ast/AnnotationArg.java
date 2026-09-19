package org.dvlang.compiler.ast;

import java.util.List;

/**
 * One argument inside an {@link AnnotationNode}'s argument list. Covers a bare
 * validator name ({@code unique}), and a call with named arguments
 * ({@code size(min: 8)}) &mdash; see grammar rule {@code annotationArg}.
 */
public sealed interface AnnotationArg extends AstNode permits AnnotationArg.Bare, AnnotationArg.Call {

    /** The argument or call name, e.g. {@code "notEmpty"} or {@code "size"}. */
    String name();

    /** {@code notEmpty}, {@code unique}, {@code uuid}. */
    record Bare(String name, SourceRange range) implements AnnotationArg {
    }

    /** {@code size(min: 8)}, {@code size(min: 8, max: 20)}. */
    record Call(String name, List<NamedArg> namedArgs, SourceRange range) implements AnnotationArg {
    }

    /** One {@code name: value} pair inside a {@link Call}. Not an {@link AstNode} on its own. */
    record NamedArg(String name, String value, SourceRange range) {
    }
}
