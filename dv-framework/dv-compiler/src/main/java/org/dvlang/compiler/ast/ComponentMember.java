package org.dvlang.compiler.ast;

import java.util.List;

/** One member of a {@code @Component} body: {@code ui:}, {@code title:}, {@code fields {}}, {@code onSubmit:}. */
public sealed interface ComponentMember extends AstNode
        permits ComponentMember.UiProp, ComponentMember.TitleProp,
        ComponentMember.FieldsBlock, ComponentMember.OnSubmitProp {

    /** {@code ui: FormLogin} */
    record UiProp(String uiKind, SourceRange range) implements ComponentMember {
    }

    /** {@code title: "Please login"} */
    record TitleProp(String value, SourceRange range) implements ComponentMember {
    }

    /** {@code fields { userName password (passwordField) }} */
    record FieldsBlock(List<FieldRef> fields, SourceRange range) implements ComponentMember {
    }

    /** {@code onSubmit: User.login} */
    record OnSubmitProp(List<String> qualifiedName, SourceRange range) implements ComponentMember {

        public String targetName() {
            return qualifiedName.isEmpty() ? "" : qualifiedName.get(0);
        }

        public String memberName() {
            return qualifiedName.size() < 2 ? "" : qualifiedName.get(1);
        }
    }
}
