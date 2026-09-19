# DV Framework

Stateless, server-driven full-stack framework where one `.dv` file declares domain,
database access and UI, and the runtime serves a working SPA with no manual frontend
build. Full spec: [`docs/DV-Framework-Coder-Brief-v1.pdf`](docs/DV-Framework-Coder-Brief-v1.pdf)
(Java 25, Maven multi-module, Vert.x 5, ANTLR4, LSP4J, GraalVM polyglot, PostgreSQL,
Redis, esbuild — see brief section 2 for the complete stack).

This tree is separate from the rest of this repository (Foblex Flow, an Angular
component library): different language, different build tool, different runtime.
It lives here as its own subtree rather than a new repository.

## Status: Milestone 1 only

> Parent POM, modules, grammar, AST, symbol table, diagnostics. Passes when: example
> files parse to the model; 20 broken fixtures each give the expected diagnostic with
> correct range; no fixture throws.

Implemented in `dv-compiler`:

- ANTLR4 grammar (`DvLexer.g4` + `DvParser.g4`) for `@Domain`, `@Database`,
  `@Component`, `@JS`/`@React`/`@Lit` raw blocks.
- Sealed-interface/record AST, every node carrying a `SourceRange`.
- A parser facade that never throws: ANTLR's default error recovery stays on, syntax
  errors become diagnostics, and any unexpected exception is still caught as a
  last-resort backstop.
- A workspace-level `SymbolTable` resolving domain/query/component references across
  files.
- `SemanticAnalyzer` implementing the eight v1 checks: unknown annotation, unknown
  validator, bad validator argument, duplicate declaration, unresolved domain/query
  reference, field not in the bound domain, `@secret` misuse in an
  exists/one/list query, and unknown UI kind (this last one is hookable but disabled
  by default — see below).
- `Workspace.update(uri, text)` → `CompilationResult(model, diagnostics, symbols)`,
  the shared entry point for the future LSP server and runtime.

`dv-emitter-react`, `dv-emitter-lit`, `dv-lsp`, `dv-runtime`, `dv-cli` are module
skeletons only (POM + a `package-info.java` noting the target milestone). No code yet.

`dv-example/` (`User.dv`, `UserLogin.dv`) is not a Maven module — it is consumed
directly by `dv-compiler`'s tests via a relative path, matching the module table.

## Known gaps against the brief

- **Java version**: the brief specifies Java 25. This environment only has a Java 21
  JDK installed, so `maven.compiler.release` is pinned to 21 in the parent POM to keep
  the build reproducible here. Bump it (and re-pin plugin/dependency versions as
  needed) once a Java 25 toolchain is available — nothing in the code depends on a
  Java 21-only feature.
- **UI kind check (section 3, "UI kind not present in the active component map")**:
  there is no real component map yet (that's milestone M4), so `Workspace.update`
  constructs `SemanticAnalyzer` with the check disabled (`null` known-kind set). The
  check itself is implemented and covered by tests, which pass an explicit
  `SemanticAnalyzer.DEFAULT_UI_KINDS` placeholder set directly.
- **`@React` / `@Lit` override block binding**: the brief doesn't give a concrete
  syntax for how one of these blocks names the component it overrides. See
  `dv-compiler/src/main/antlr4/org/dvlang/compiler/parser/NOTE.md` for the smallest
  option taken and what would need sign-off to change it (working rule, brief
  section 11).

## Building

```sh
cd dv-framework
mvn install
```

Runs the full reactor. `dv-compiler`'s tests are the M1 acceptance checks described
above; run them alone with `mvn -pl dv-compiler -am test`.
