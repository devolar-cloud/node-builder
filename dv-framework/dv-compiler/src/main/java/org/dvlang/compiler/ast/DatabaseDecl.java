package org.dvlang.compiler.ast;

import java.util.List;

/** {@code @Database(User, "users") { crud query login(userName, password): verify }}. */
public record DatabaseDecl(
        String domainName,
        String tableName,
        boolean hasCrud,
        List<QueryDecl> queries,
        SourceRange range
) implements TopLevelDecl {
}
