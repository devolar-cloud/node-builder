package org.dvlang.compiler.ast;

import java.util.List;

/** {@code @id(uuid)}, {@code @validator(notEmpty, size(min: 8), unique)}, {@code @secret}. */
public record AnnotationNode(String name, List<AnnotationArg> args, SourceRange range) implements AstNode {
}
