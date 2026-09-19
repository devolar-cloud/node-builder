package org.dvlang.compiler.ast;

import java.util.List;
import java.util.Optional;

/** {@code @Component var UserLogin { ... }}. */
public record ComponentDecl(String name, List<ComponentMember> members, SourceRange range) implements TopLevelDecl {

    public Optional<ComponentMember.UiProp> uiProp() {
        return members.stream()
                .filter(ComponentMember.UiProp.class::isInstance)
                .map(ComponentMember.UiProp.class::cast)
                .findFirst();
    }

    public Optional<ComponentMember.FieldsBlock> fieldsBlock() {
        return members.stream()
                .filter(ComponentMember.FieldsBlock.class::isInstance)
                .map(ComponentMember.FieldsBlock.class::cast)
                .findFirst();
    }

    public Optional<ComponentMember.OnSubmitProp> onSubmitProp() {
        return members.stream()
                .filter(ComponentMember.OnSubmitProp.class::isInstance)
                .map(ComponentMember.OnSubmitProp.class::cast)
                .findFirst();
    }
}
