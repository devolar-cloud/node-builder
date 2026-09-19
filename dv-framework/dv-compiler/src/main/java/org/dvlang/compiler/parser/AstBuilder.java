package org.dvlang.compiler.parser;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.dvlang.compiler.ast.AnnotationArg;
import org.dvlang.compiler.ast.AnnotationNode;
import org.dvlang.compiler.ast.ComponentDecl;
import org.dvlang.compiler.ast.ComponentMember;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.ast.DatabaseDecl;
import org.dvlang.compiler.ast.DomainDecl;
import org.dvlang.compiler.ast.FieldDecl;
import org.dvlang.compiler.ast.FieldRef;
import org.dvlang.compiler.ast.JsBlockDecl;
import org.dvlang.compiler.ast.QueryDecl;
import org.dvlang.compiler.ast.QueryKind;
import org.dvlang.compiler.ast.RawTemplateDecl;
import org.dvlang.compiler.ast.SourceRange;
import org.dvlang.compiler.ast.TemplateTarget;
import org.dvlang.compiler.ast.TopLevelDecl;

/**
 * Turns an ANTLR parse tree into the sealed-record AST. Every method here is
 * defensive against null sub-contexts: ANTLR's error recovery can leave a rule
 * partially matched, and this builder must never throw on that (brief section 3,
 * compiler core requirement 1) &mdash; a missing piece is simply omitted or left
 * empty/null rather than raising.
 */
final class AstBuilder {

    private final String fileUri;

    AstBuilder(String fileUri) {
        this.fileUri = fileUri;
    }

    CompilationUnit build(DvParser.CompilationUnitContext ctx) {
        List<TopLevelDecl> decls = new ArrayList<>();
        if (ctx != null) {
            for (DvParser.TopLevelDeclContext tld : ctx.topLevelDecl()) {
                TopLevelDecl decl = buildTopLevelDecl(tld);
                if (decl != null) {
                    decls.add(decl);
                }
            }
        }
        return new CompilationUnit(fileUri, List.copyOf(decls), rangeOf(ctx));
    }

    private TopLevelDecl buildTopLevelDecl(DvParser.TopLevelDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.domainDecl() != null) {
            return buildDomain(ctx.domainDecl());
        }
        if (ctx.databaseDecl() != null) {
            return buildDatabase(ctx.databaseDecl());
        }
        if (ctx.componentDecl() != null) {
            return buildComponent(ctx.componentDecl());
        }
        if (ctx.jsBlockDecl() != null) {
            return buildJsBlock(ctx.jsBlockDecl());
        }
        if (ctx.rawTemplateDecl() != null) {
            return buildRawTemplate(ctx.rawTemplateDecl());
        }
        return null;
    }

    private DomainDecl buildDomain(DvParser.DomainDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<FieldDecl> fields = new ArrayList<>();
        for (DvParser.FieldDeclContext fd : ctx.fieldDecl()) {
            FieldDecl field = buildField(fd);
            if (field != null) {
                fields.add(field);
            }
        }
        return new DomainDecl(textOf(ctx.ID()), List.copyOf(fields), rangeOf(ctx));
    }

    private FieldDecl buildField(DvParser.FieldDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<AnnotationNode> annotations = new ArrayList<>();
        for (DvParser.AnnotationContext a : ctx.annotation()) {
            AnnotationNode annotation = buildAnnotation(a);
            if (annotation != null) {
                annotations.add(annotation);
            }
        }
        return new FieldDecl(textOf(ctx.ID()), List.copyOf(annotations), rangeOf(ctx));
    }

    private AnnotationNode buildAnnotation(DvParser.AnnotationContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<AnnotationArg> args = new ArrayList<>();
        if (ctx.annotationArgList() != null) {
            for (DvParser.AnnotationArgContext a : ctx.annotationArgList().annotationArg()) {
                AnnotationArg arg = buildAnnotationArg(a);
                if (arg != null) {
                    args.add(arg);
                }
            }
        }
        return new AnnotationNode(textOf(ctx.ID()), List.copyOf(args), rangeOf(ctx));
    }

    private AnnotationArg buildAnnotationArg(DvParser.AnnotationArgContext ctx) {
        if (ctx == null) {
            return null;
        }
        String name = textOf(ctx.ID());
        if (ctx.LPAREN() != null) {
            List<AnnotationArg.NamedArg> named = new ArrayList<>();
            if (ctx.namedArgList() != null) {
                for (DvParser.NamedArgContext na : ctx.namedArgList().namedArg()) {
                    AnnotationArg.NamedArg namedArg = buildNamedArg(na);
                    if (namedArg != null) {
                        named.add(namedArg);
                    }
                }
            }
            return new AnnotationArg.Call(name, List.copyOf(named), rangeOf(ctx));
        }
        return new AnnotationArg.Bare(name, rangeOf(ctx));
    }

    private AnnotationArg.NamedArg buildNamedArg(DvParser.NamedArgContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new AnnotationArg.NamedArg(textOf(ctx.ID()), literalText(ctx.literal()), rangeOf(ctx));
    }

    private String literalText(DvParser.LiteralContext ctx) {
        if (ctx == null) {
            return "";
        }
        if (ctx.STRING() != null) {
            return unquote(ctx.STRING().getText());
        }
        if (ctx.NUMBER() != null) {
            return ctx.NUMBER().getText();
        }
        if (ctx.ID() != null) {
            return ctx.ID().getText();
        }
        return "";
    }

    private DatabaseDecl buildDatabase(DvParser.DatabaseDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        boolean hasCrud = false;
        List<QueryDecl> queries = new ArrayList<>();
        for (DvParser.DatabaseMemberContext member : ctx.databaseMember()) {
            if (member == null) {
                continue;
            }
            if (member.CRUD() != null) {
                hasCrud = true;
            } else if (member.queryDecl() != null) {
                QueryDecl query = buildQuery(member.queryDecl());
                if (query != null) {
                    queries.add(query);
                }
            }
        }
        return new DatabaseDecl(textOf(ctx.ID()), unquote(textOf(ctx.STRING())), hasCrud, List.copyOf(queries), rangeOf(ctx));
    }

    private QueryDecl buildQuery(DvParser.QueryDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<String> params = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (TerminalNode id : ctx.paramList().ID()) {
                params.add(id.getText());
            }
        }
        return new QueryDecl(textOf(ctx.ID()), List.copyOf(params), queryKind(ctx.queryKind()), rangeOf(ctx));
    }

    private QueryKind queryKind(DvParser.QueryKindContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.VERIFY() != null) {
            return QueryKind.VERIFY;
        }
        if (ctx.EXISTS() != null) {
            return QueryKind.EXISTS;
        }
        if (ctx.ONE() != null) {
            return QueryKind.ONE;
        }
        if (ctx.LIST() != null) {
            return QueryKind.LIST;
        }
        return null;
    }

    private ComponentDecl buildComponent(DvParser.ComponentDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<ComponentMember> members = new ArrayList<>();
        for (DvParser.ComponentMemberContext cm : ctx.componentMember()) {
            ComponentMember member = buildComponentMember(cm);
            if (member != null) {
                members.add(member);
            }
        }
        return new ComponentDecl(textOf(ctx.ID()), List.copyOf(members), rangeOf(ctx));
    }

    private ComponentMember buildComponentMember(DvParser.ComponentMemberContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.uiProp() != null) {
            DvParser.UiPropContext uiProp = ctx.uiProp();
            return new ComponentMember.UiProp(textOf(uiProp.ID()), rangeOf(uiProp));
        }
        if (ctx.titleProp() != null) {
            DvParser.TitlePropContext titleProp = ctx.titleProp();
            return new ComponentMember.TitleProp(unquote(textOf(titleProp.STRING())), rangeOf(titleProp));
        }
        if (ctx.fieldsBlock() != null) {
            return buildFieldsBlock(ctx.fieldsBlock());
        }
        if (ctx.onSubmitProp() != null) {
            return buildOnSubmit(ctx.onSubmitProp());
        }
        return null;
    }

    private ComponentMember.FieldsBlock buildFieldsBlock(DvParser.FieldsBlockContext ctx) {
        List<FieldRef> fields = new ArrayList<>();
        for (DvParser.FieldRefContext fr : ctx.fieldRef()) {
            FieldRef fieldRef = buildFieldRef(fr);
            if (fieldRef != null) {
                fields.add(fieldRef);
            }
        }
        return new ComponentMember.FieldsBlock(List.copyOf(fields), rangeOf(ctx));
    }

    private FieldRef buildFieldRef(DvParser.FieldRefContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<TerminalNode> ids = ctx.ID();
        String name = ids.isEmpty() ? "" : ids.get(0).getText();
        String widget = ids.size() > 1 ? ids.get(1).getText() : null;
        return new FieldRef(name, widget, rangeOf(ctx));
    }

    private ComponentMember.OnSubmitProp buildOnSubmit(DvParser.OnSubmitPropContext ctx) {
        List<String> parts = new ArrayList<>();
        if (ctx.qualifiedName() != null) {
            for (TerminalNode id : ctx.qualifiedName().ID()) {
                parts.add(id.getText());
            }
        }
        return new ComponentMember.OnSubmitProp(List.copyOf(parts), rangeOf(ctx));
    }

    private JsBlockDecl buildJsBlock(DvParser.JsBlockDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new JsBlockDecl(rawBodyOf(ctx.JS_KW()), rangeOf(ctx));
    }

    private RawTemplateDecl buildRawTemplate(DvParser.RawTemplateDeclContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.REACT_KW() != null) {
            return new RawTemplateDecl(TemplateTarget.REACT, rawBodyOf(ctx.REACT_KW()), rangeOf(ctx));
        }
        if (ctx.LIT_KW() != null) {
            return new RawTemplateDecl(TemplateTarget.LIT, rawBodyOf(ctx.LIT_KW()), rangeOf(ctx));
        }
        return null;
    }

    /**
     * {@code @JS}/{@code @React}/{@code @Lit} lex as one opaque token spanning the
     * keyword through the matching closing brace; this extracts the text strictly
     * between the outer braces.
     */
    private String rawBodyOf(TerminalNode token) {
        if (token == null) {
            return "";
        }
        String text = token.getText();
        if (text == null) {
            return "";
        }
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first < 0 || last <= first) {
            return "";
        }
        return text.substring(first + 1, last);
    }

    private static String textOf(TerminalNode node) {
        return node == null ? "" : node.getText();
    }

    private static String unquote(String text) {
        if (text == null || text.length() < 2 || text.charAt(0) != '"') {
            return text == null ? "" : text;
        }
        return text.substring(1, text.length() - 1);
    }

    private SourceRange rangeOf(ParserRuleContext ctx) {
        if (ctx == null) {
            return SourceRange.of(fileUri, 0, 0);
        }
        Token start = ctx.getStart();
        Token stop = ctx.getStop() != null ? ctx.getStop() : start;
        if (start == null) {
            return SourceRange.of(fileUri, 0, 0);
        }
        SourceRange startRange = rangeOf(start);
        SourceRange stopRange = rangeOf(stop);
        return new SourceRange(fileUri, startRange.startLine(), startRange.startColumn(), stopRange.endLine(), stopRange.endColumn());
    }

    private SourceRange rangeOf(TerminalNode node) {
        return node == null ? SourceRange.of(fileUri, 0, 0) : rangeOf(node.getSymbol());
    }

    /** Handles multi-line tokens (the opaque {@code @JS}/{@code @React}/{@code @Lit} blocks). */
    private SourceRange rangeOf(Token token) {
        if (token == null) {
            return SourceRange.of(fileUri, 0, 0);
        }
        String text = token.getText() != null ? token.getText() : "";
        int startLine = token.getLine();
        int startColumn = token.getCharPositionInLine();
        int endLine = startLine;
        int endColumn = startColumn + text.length();
        int newlineCount = 0;
        int lastNewlineIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                newlineCount++;
                lastNewlineIndex = i;
            }
        }
        if (newlineCount > 0) {
            endLine = startLine + newlineCount;
            endColumn = text.length() - lastNewlineIndex - 1;
        }
        return new SourceRange(fileUri, startLine, startColumn, endLine, endColumn);
    }
}
