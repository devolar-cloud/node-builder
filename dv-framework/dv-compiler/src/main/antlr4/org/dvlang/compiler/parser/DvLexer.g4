lexer grammar DvLexer;

// Split out from the parser grammar (Dv.g4) because ANTLR4 only allows lexical
// `mode` declarations in a standalone lexer grammar, and the @JS/@React/@Lit raw
// blocks need modes (see below and NOTE.md).

// JS_KW / REACT_KW / LIT_KW are declared here so the `type(...)` commands in the
// RAW_* modes below can retarget a raw block's final token to them; those three
// rules never complete on their own (they always `more` into a RAW_* mode).
tokens { JS_KW, REACT_KW, LIT_KW }

@members {
    private int balanceDepth = 0;
}

// Keywords that introduce a top-level declaration are matched as whole literals so
// they win over the generic AT + ID pattern by ANTLR's longest-match rule.
DOMAIN_KW    : '@Domain' ;
DATABASE_KW  : '@Database' ;
COMPONENT_KW : '@Component' ;

// @JS / @React / @Lit blocks: the keyword through the matching closing brace is a
// single token, counting nested braces so the body's own '{' '}' pairs (e.g. a
// function body) don't end the block early. Each uses its own mode (rather than one
// shared mode with a predicated loop) because a semantic predicate gating a `*` loop
// inside a single rule is evaluated during the lexer's speculative DFA/ATN lookahead,
// before the counting actions actually run, and unreliably under-consumes; a mode per
// keyword with a two-alternative gated pair on '}' only ever needs one character of
// lookahead to disambiguate; see NOTE.md.
JS_KW    : '@JS' [ \t\r\n]* '{' { balanceDepth = 1; } -> more, pushMode(RAW_JS) ;
REACT_KW : '@React' [ \t\r\n]* '{' { balanceDepth = 1; } -> more, pushMode(RAW_REACT) ;
LIT_KW   : '@Lit' [ \t\r\n]* '{' { balanceDepth = 1; } -> more, pushMode(RAW_LIT) ;

AT : '@' ;

VAR       : 'var' ;
CRUD      : 'crud' ;
QUERY     : 'query' ;
UI        : 'ui' ;
TITLE     : 'title' ;
FIELDS    : 'fields' ;
ONSUBMIT  : 'onSubmit' ;
VERIFY    : 'verify' ;
EXISTS    : 'exists' ;
ONE       : 'one' ;
LIST      : 'list' ;

LBRACE : '{' ;
RBRACE : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
COMMA  : ',' ;
COLON  : ':' ;
DOT    : '.' ;

STRING : '"' (~["\\\r\n] | '\\' .)* '"' ;
NUMBER : [0-9]+ ;
ID     : [a-zA-Z_][a-zA-Z0-9_]* ;

WS            : [ \t\r\n]+ -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

// ---------------------------------------------------------------------------------
// Raw block modes (see JS_KW / REACT_KW / LIT_KW above and NOTE.md)
// ---------------------------------------------------------------------------------

mode RAW_JS;
RAW_JS_OPEN        : '{' { balanceDepth++; } -> more ;
RAW_JS_CLOSE_INNER : '}' {balanceDepth > 1}? { balanceDepth--; } -> more ;
RAW_JS_CLOSE_FINAL : '}' {balanceDepth == 1}? { balanceDepth--; } -> type(JS_KW), popMode ;
RAW_JS_ANY         : . -> more ;

mode RAW_REACT;
RAW_REACT_OPEN        : '{' { balanceDepth++; } -> more ;
RAW_REACT_CLOSE_INNER : '}' {balanceDepth > 1}? { balanceDepth--; } -> more ;
RAW_REACT_CLOSE_FINAL : '}' {balanceDepth == 1}? { balanceDepth--; } -> type(REACT_KW), popMode ;
RAW_REACT_ANY         : . -> more ;

mode RAW_LIT;
RAW_LIT_OPEN        : '{' { balanceDepth++; } -> more ;
RAW_LIT_CLOSE_INNER : '}' {balanceDepth > 1}? { balanceDepth--; } -> more ;
RAW_LIT_CLOSE_FINAL : '}' {balanceDepth == 1}? { balanceDepth--; } -> type(LIT_KW), popMode ;
RAW_LIT_ANY         : . -> more ;
