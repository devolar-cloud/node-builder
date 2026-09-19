package org.dvlang.compiler.ast;

/**
 * {@code @React { ... }} / {@code @Lit { ... }}: a hand-written template that
 * replaces the generated one for that target (brief section 3). See the open design
 * point in {@code parser/NOTE.md} about how this binds to a specific component.
 */
public record RawTemplateDecl(TemplateTarget target, String rawBody, SourceRange range) implements TopLevelDecl {
}
