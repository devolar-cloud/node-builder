# Open design point: `@React` / `@Lit` override block binding

Brief section 3 introduces "optional per-component override blocks: `@React { ... }`
and `@Lit { ... }` hold a hand-written template for that target and replace the
generated one," but gives no concrete syntax for how a block names the component it
overrides, unlike the fully worked `@Domain` / `@Database` / `@Component` / `@JS`
examples.

Smallest option taken for v1 (per the working rule in section 11: "propose the
smallest option in a short note and continue"): reuse the exact same opaque,
balanced-brace token shape as `@JS`. The block parses (`RawTemplateDecl` in the AST)
but is not yet bound to a specific component by name — that wiring is emitter work
for milestone M4, where `RawTemplate(target, source)` from the UI IR (section 4) is
produced. Revisit this note once M4 defines the concrete binding syntax; needs
sign-off before the language grows beyond section 3 as written.
