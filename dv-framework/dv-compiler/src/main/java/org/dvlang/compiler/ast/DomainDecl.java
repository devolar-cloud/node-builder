package org.dvlang.compiler.ast;

import java.util.List;

/** {@code @Domain var User { ... }}. */
public record DomainDecl(String name, List<FieldDecl> fields, SourceRange range) implements TopLevelDecl {

    public boolean hasField(String fieldName) {
        return fields.stream().anyMatch(f -> f.name().equals(fieldName));
    }
}
