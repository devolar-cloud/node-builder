package org.dvlang.compiler.ast;

import java.util.List;

/** One field inside a {@link DomainDecl}, e.g. {@code @validator(notEmpty) userName}. */
public record FieldDecl(String name, List<AnnotationNode> annotations, SourceRange range) implements AstNode {

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream().anyMatch(a -> a.name().equals(annotationName));
    }
}
