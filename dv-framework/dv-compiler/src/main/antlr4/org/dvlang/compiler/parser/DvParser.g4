parser grammar DvParser;

options {
    tokenVocab = DvLexer;
}

// DV language grammar (brief section 3). Normalises the founder's sketch: braces,
// consistent annotation arguments. String is the default field type (no field-type
// syntax in v1).
//
// ANTLR's default error recovery stays on: a malformed file still yields a partial
// parse tree instead of throwing, which is what lets the compiler core satisfy
// "parsing never throws" (section 3, compiler core requirement 1).

compilationUnit
    : topLevelDecl* EOF
    ;

topLevelDecl
    : domainDecl
    | databaseDecl
    | componentDecl
    | jsBlockDecl
    | rawTemplateDecl
    ;

domainDecl
    : DOMAIN_KW VAR ID LBRACE fieldDecl* RBRACE
    ;

fieldDecl
    : annotation* ID
    ;

annotation
    : AT ID (LPAREN annotationArgList? RPAREN)?
    ;

annotationArgList
    : annotationArg (COMMA annotationArg)*
    ;

// Covers both a bare validator name (notEmpty, unique) and a call with named
// arguments (size(min: 8), size(min: 8, max: 20)).
annotationArg
    : ID (LPAREN namedArgList? RPAREN)?
    ;

namedArgList
    : namedArg (COMMA namedArg)*
    ;

namedArg
    : ID COLON literal
    ;

literal
    : STRING
    | NUMBER
    | ID
    ;

databaseDecl
    : DATABASE_KW LPAREN ID COMMA STRING RPAREN LBRACE databaseMember* RBRACE
    ;

databaseMember
    : CRUD
    | queryDecl
    ;

queryDecl
    : QUERY ID LPAREN paramList? RPAREN COLON queryKind
    ;

paramList
    : ID (COMMA ID)*
    ;

queryKind
    : VERIFY
    | EXISTS
    | ONE
    | LIST
    ;

componentDecl
    : COMPONENT_KW VAR ID LBRACE componentMember* RBRACE
    ;

componentMember
    : uiProp
    | titleProp
    | fieldsBlock
    | onSubmitProp
    ;

uiProp
    : UI COLON ID
    ;

titleProp
    : TITLE COLON STRING
    ;

fieldsBlock
    : FIELDS LBRACE fieldRef* RBRACE
    ;

fieldRef
    : ID (LPAREN ID RPAREN)?
    ;

onSubmitProp
    : ONSUBMIT COLON qualifiedName
    ;

qualifiedName
    : ID (DOT ID)*
    ;

// @JS { ... } is lexed as one opaque token (JS_KW, see DvLexer.g4): the body is not
// parsed in v1 (compiler core requirement, section 3).
jsBlockDecl
    : JS_KW
    ;

// Optional per-component override blocks (section 3): @React { ... } and @Lit { ... }
// hold a hand-written template for that target. The brief does not fix how the block
// binds to a component; per the working rule in section 11 ("propose the smallest
// option in a short note and continue"), v1 reuses the same opaque balanced-brace
// token shape as @JS. See NOTE.md next to this grammar.
rawTemplateDecl
    : REACT_KW
    | LIT_KW
    ;
