package org.dvlang.compiler.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.dvlang.compiler.ast.AnnotationArg;
import org.dvlang.compiler.ast.AnnotationNode;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.ast.ComponentDecl;
import org.dvlang.compiler.ast.ComponentMember;
import org.dvlang.compiler.ast.DatabaseDecl;
import org.dvlang.compiler.ast.DomainDecl;
import org.dvlang.compiler.ast.FieldDecl;
import org.dvlang.compiler.ast.FieldRef;
import org.dvlang.compiler.ast.QueryDecl;
import org.dvlang.compiler.ast.QueryKind;
import org.dvlang.compiler.ast.TopLevelDecl;
import org.dvlang.compiler.diagnostics.Diagnostic;
import org.dvlang.compiler.diagnostics.DiagnosticCodes;
import org.dvlang.compiler.symbols.SymbolTable;

/**
 * The eight v1 semantic checks (brief section 3): unknown annotation, unknown
 * validator, bad validator argument, duplicate declaration, unresolved domain or
 * query reference, field in a component not present in the bound domain, a
 * {@code @secret} field used in an {@code exists}/{@code one}/{@code list} query, and
 * UI kind not present in the active component map. Never throws for a user error
 * (compiler core requirement 4): every check is a defensive lookup, not an assertion.
 *
 * <p>A component's "bound domain" is inferred from its {@code onSubmit} target (the
 * only binding shown in the brief's worked example, section 3): {@code onSubmit:
 * User.login} binds the component to the {@code User} domain. A component with no
 * {@code onSubmit} has nothing to check its {@code fields} against.
 */
public final class SemanticAnalyzer {

    private static final Set<String> KNOWN_ANNOTATIONS = Set.of("id", "validator", "secret");

    /** Built-ins from brief section 8. */
    private static final Set<String> KNOWN_VALIDATORS =
            Set.of("notEmpty", "size", "email", "pattern", "unique", "min", "max");

    private static final Set<String> ZERO_ARG_VALIDATORS = Set.of("notEmpty", "unique", "email");

    private static final Set<String> SIZE_ARG_NAMES = Set.of("min", "max");

    /**
     * Placeholder UI kind set for tests and fixtures until a real component map is
     * wired in at milestone M4; {@link #SemanticAnalyzer(SymbolTable)} leaves the
     * check disabled ({@code null}) because v1 has no "active component map" yet.
     */
    public static final Set<String> DEFAULT_UI_KINDS = Set.of("FormLogin", "FormCreate", "FormEdit", "Table", "Detail");

    private final SymbolTable symbols;
    private final Set<String> knownUiKinds;

    public SemanticAnalyzer(SymbolTable symbols) {
        this(symbols, null);
    }

    public SemanticAnalyzer(SymbolTable symbols, Set<String> knownUiKinds) {
        this.symbols = symbols;
        this.knownUiKinds = knownUiKinds;
    }

    public List<Diagnostic> analyze(String fileUri, CompilationUnit unit) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (unit == null) {
            return diagnostics;
        }
        for (TopLevelDecl decl : unit.declarations()) {
            switch (decl) {
                case DomainDecl d -> analyzeDomain(d, diagnostics);
                case DatabaseDecl db -> analyzeDatabase(db, diagnostics);
                case ComponentDecl c -> analyzeComponent(c, diagnostics);
                default -> {
                    // @JS / @React / @Lit bodies are opaque in v1; nothing to check.
                }
            }
        }
        return diagnostics;
    }

    private void analyzeDomain(DomainDecl domain, List<Diagnostic> diagnostics) {
        if (symbols.domainDeclarations(domain.name()).size() > 1) {
            diagnostics.add(Diagnostic.error(
                    DiagnosticCodes.DUPLICATE_DECLARATION,
                    "duplicate domain '" + domain.name() + "'",
                    domain.range()));
        }

        Set<String> seenFields = new HashSet<>();
        for (FieldDecl field : domain.fields()) {
            if (!seenFields.add(field.name())) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.DUPLICATE_DECLARATION,
                        "duplicate field '" + field.name() + "' in domain '" + domain.name() + "'",
                        field.range()));
            }
            for (AnnotationNode annotation : field.annotations()) {
                analyzeAnnotation(annotation, diagnostics);
            }
        }
    }

    private void analyzeAnnotation(AnnotationNode annotation, List<Diagnostic> diagnostics) {
        if (!KNOWN_ANNOTATIONS.contains(annotation.name())) {
            diagnostics.add(Diagnostic.error(
                    DiagnosticCodes.UNKNOWN_ANNOTATION,
                    "unknown annotation '@" + annotation.name() + "'",
                    annotation.range()));
            return;
        }
        if (!annotation.name().equals("validator")) {
            return;
        }
        for (AnnotationArg arg : annotation.args()) {
            analyzeValidatorArg(arg, diagnostics);
        }
    }

    private void analyzeValidatorArg(AnnotationArg arg, List<Diagnostic> diagnostics) {
        String name = arg.name();
        if (!KNOWN_VALIDATORS.contains(name)) {
            diagnostics.add(Diagnostic.error(
                    DiagnosticCodes.UNKNOWN_VALIDATOR,
                    "unknown validator '" + name + "'",
                    arg.range()));
            return;
        }
        if (ZERO_ARG_VALIDATORS.contains(name)) {
            if (arg instanceof AnnotationArg.Call) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.BAD_VALIDATOR_ARGUMENT,
                        "validator '" + name + "' takes no arguments",
                        arg.range()));
            }
            return;
        }
        if (name.equals("size") && arg instanceof AnnotationArg.Call call) {
            for (AnnotationArg.NamedArg namedArg : call.namedArgs()) {
                if (!SIZE_ARG_NAMES.contains(namedArg.name()) || !isNonNegativeInteger(namedArg.value())) {
                    diagnostics.add(Diagnostic.error(
                            DiagnosticCodes.BAD_VALIDATOR_ARGUMENT,
                            "validator 'size' takes numeric min/max arguments, got '"
                                    + namedArg.name() + ": " + namedArg.value() + "'",
                            arg.range()));
                }
            }
            return;
        }
        if (name.equals("pattern")) {
            if (!(arg instanceof AnnotationArg.Call call) || call.namedArgs().size() != 1) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.BAD_VALIDATOR_ARGUMENT,
                        "validator 'pattern' requires exactly one argument",
                        arg.range()));
            }
        }
        // 'min' / 'max' as standalone validators have no worked example in the brief;
        // their argument shape is left unvalidated in v1 (see parser/NOTE.md policy).
    }

    private static boolean isNonNegativeInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void analyzeDatabase(DatabaseDecl database, List<Diagnostic> diagnostics) {
        Optional<DomainDecl> domain = symbols.resolveDomain(database.domainName());
        if (domain.isEmpty()) {
            diagnostics.add(Diagnostic.error(
                    DiagnosticCodes.UNRESOLVED_REFERENCE,
                    "unresolved domain '" + database.domainName() + "'",
                    database.range()));
        }

        Set<String> seenQueries = new HashSet<>();
        for (QueryDecl query : database.queries()) {
            if (!seenQueries.add(query.name())) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.DUPLICATE_DECLARATION,
                        "duplicate query '" + query.name() + "' in database for '" + database.domainName() + "'",
                        query.range()));
            }
            if (domain.isPresent() && query.kind() != null && query.kind() != QueryKind.VERIFY) {
                if (referencesSecretField(domain.get(), query)) {
                    diagnostics.add(Diagnostic.error(
                            DiagnosticCodes.SECRET_FIELD_IN_QUERY,
                            "query '" + query.name() + "' (" + query.kind() + ") may not reference a @secret field",
                            query.range()));
                }
            }
        }
    }

    private boolean referencesSecretField(DomainDecl domain, QueryDecl query) {
        Map<String, FieldDecl> fieldsByName = new HashMap<>();
        for (FieldDecl field : domain.fields()) {
            fieldsByName.put(field.name(), field);
        }
        for (String param : query.params()) {
            FieldDecl field = fieldsByName.get(param);
            if (field != null && field.hasAnnotation("secret")) {
                return true;
            }
        }
        return false;
    }

    private void analyzeComponent(ComponentDecl component, List<Diagnostic> diagnostics) {
        if (symbols.componentDeclarations(component.name()).size() > 1) {
            diagnostics.add(Diagnostic.error(
                    DiagnosticCodes.DUPLICATE_DECLARATION,
                    "duplicate component '" + component.name() + "'",
                    component.range()));
        }

        Set<Class<? extends ComponentMember>> seenMemberKinds = new HashSet<>();
        for (ComponentMember member : component.members()) {
            if (!seenMemberKinds.add(member.getClass())) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.DUPLICATE_DECLARATION,
                        "duplicate member in component '" + component.name() + "'",
                        member.range()));
            }
            if (member instanceof ComponentMember.UiProp uiProp && knownUiKinds != null
                    && !knownUiKinds.contains(uiProp.uiKind())) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.UNKNOWN_UI_KIND,
                        "UI kind '" + uiProp.uiKind() + "' is not present in the active component map",
                        uiProp.range()));
            }
        }

        Optional<ComponentMember.OnSubmitProp> onSubmit = component.onSubmitProp();
        Optional<DomainDecl> boundDomain = Optional.empty();
        if (onSubmit.isPresent()) {
            ComponentMember.OnSubmitProp submit = onSubmit.get();
            Optional<DomainDecl> domain = symbols.resolveDomain(submit.targetName());
            if (domain.isEmpty()) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.UNRESOLVED_REFERENCE,
                        "unresolved domain '" + submit.targetName() + "' in onSubmit",
                        submit.range()));
            } else if (symbols.resolveQuery(submit.targetName(), submit.memberName()).isEmpty()) {
                diagnostics.add(Diagnostic.error(
                        DiagnosticCodes.UNRESOLVED_REFERENCE,
                        "unresolved query '" + submit.targetName() + "." + submit.memberName() + "' in onSubmit",
                        submit.range()));
            } else {
                boundDomain = domain;
            }
        }

        if (boundDomain.isPresent()) {
            DomainDecl domain = boundDomain.get();
            for (ComponentMember member : component.members()) {
                if (member instanceof ComponentMember.FieldsBlock fieldsBlock) {
                    for (FieldRef fieldRef : fieldsBlock.fields()) {
                        if (!domain.hasField(fieldRef.name())) {
                            diagnostics.add(Diagnostic.error(
                                    DiagnosticCodes.FIELD_NOT_IN_DOMAIN,
                                    "field '" + fieldRef.name() + "' is not present in domain '" + domain.name() + "'",
                                    fieldRef.range()));
                        }
                    }
                }
            }
        }
    }
}
