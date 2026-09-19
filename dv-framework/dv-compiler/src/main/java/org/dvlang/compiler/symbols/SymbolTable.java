package org.dvlang.compiler.symbols;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dvlang.compiler.ast.CompilationUnit;
import org.dvlang.compiler.ast.ComponentDecl;
import org.dvlang.compiler.ast.DatabaseDecl;
import org.dvlang.compiler.ast.DomainDecl;
import org.dvlang.compiler.ast.QueryDecl;
import org.dvlang.compiler.ast.TopLevelDecl;

/**
 * Workspace-level index over every known file's {@link CompilationUnit}, resolving
 * references across files: {@code @Database(User, ...)} and {@code onSubmit:
 * User.login} point at declarations that may live in a different {@code .dv} file
 * (brief section 3, compiler core requirement 5).
 *
 * <p>Each name keeps every declaration seen, not just the first: {@link
 * org.dvlang.compiler.semantic.SemanticAnalyzer} uses the full list to report
 * duplicate declarations, including duplicates that span files.
 */
public final class SymbolTable {

    private final Map<String, List<DomainSymbol>> domainsByName;
    private final Map<String, List<ComponentSymbol>> componentsByName;
    private final Map<String, List<DatabaseSymbol>> databasesByDomainName;

    public record DomainSymbol(String fileUri, DomainDecl decl) {
    }

    public record ComponentSymbol(String fileUri, ComponentDecl decl) {
    }

    public record DatabaseSymbol(String fileUri, DatabaseDecl decl) {
    }

    private SymbolTable(
            Map<String, List<DomainSymbol>> domainsByName,
            Map<String, List<ComponentSymbol>> componentsByName,
            Map<String, List<DatabaseSymbol>> databasesByDomainName) {
        this.domainsByName = domainsByName;
        this.componentsByName = componentsByName;
        this.databasesByDomainName = databasesByDomainName;
    }

    public static SymbolTable build(Map<String, CompilationUnit> units) {
        Map<String, List<DomainSymbol>> domains = new LinkedHashMap<>();
        Map<String, List<ComponentSymbol>> components = new LinkedHashMap<>();
        Map<String, List<DatabaseSymbol>> databases = new LinkedHashMap<>();

        for (Map.Entry<String, CompilationUnit> entry : units.entrySet()) {
            String fileUri = entry.getKey();
            CompilationUnit unit = entry.getValue();
            if (unit == null) {
                continue;
            }
            for (TopLevelDecl decl : unit.declarations()) {
                switch (decl) {
                    case DomainDecl d -> domains.computeIfAbsent(d.name(), k -> new ArrayList<>())
                            .add(new DomainSymbol(fileUri, d));
                    case ComponentDecl c -> components.computeIfAbsent(c.name(), k -> new ArrayList<>())
                            .add(new ComponentSymbol(fileUri, c));
                    case DatabaseDecl db -> databases.computeIfAbsent(db.domainName(), k -> new ArrayList<>())
                            .add(new DatabaseSymbol(fileUri, db));
                    default -> {
                        // @JS / @React / @Lit blocks are not indexed: they are not referenced by name.
                    }
                }
            }
        }
        return new SymbolTable(domains, components, databases);
    }

    public List<DomainSymbol> domainDeclarations(String name) {
        return domainsByName.getOrDefault(name, List.of());
    }

    public List<ComponentSymbol> componentDeclarations(String name) {
        return componentsByName.getOrDefault(name, List.of());
    }

    public List<DatabaseSymbol> databaseDeclarations(String domainName) {
        return databasesByDomainName.getOrDefault(domainName, List.of());
    }

    public Optional<DomainDecl> resolveDomain(String name) {
        List<DomainSymbol> found = domainsByName.get(name);
        return found == null || found.isEmpty() ? Optional.empty() : Optional.of(found.get(0).decl());
    }

    public Optional<ComponentDecl> resolveComponent(String name) {
        List<ComponentSymbol> found = componentsByName.get(name);
        return found == null || found.isEmpty() ? Optional.empty() : Optional.of(found.get(0).decl());
    }

    /** A query declared directly on any {@code @Database(domainName, ...)} block for that domain. */
    public Optional<QueryDecl> resolveQuery(String domainName, String queryName) {
        for (DatabaseSymbol database : databaseDeclarations(domainName)) {
            for (QueryDecl query : database.decl().queries()) {
                if (query.name().equals(queryName)) {
                    return Optional.of(query);
                }
            }
        }
        return Optional.empty();
    }

    public boolean hasCrud(String domainName) {
        return databaseDeclarations(domainName).stream().anyMatch(db -> db.decl().hasCrud());
    }
}
