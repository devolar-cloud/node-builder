package org.dvlang.compiler.diagnostics;

/**
 * Diagnostic codes for v1 (brief section 3). {@link #PARSE_ERROR} covers requirement 1
 * (a half-typed file still returns a partial AST plus diagnostics); the rest are the
 * eight semantic checks the section lists.
 */
public final class DiagnosticCodes {

    public static final String PARSE_ERROR = "DV0000";

    public static final String UNKNOWN_ANNOTATION = "DV0001";
    public static final String UNKNOWN_VALIDATOR = "DV0002";
    public static final String BAD_VALIDATOR_ARGUMENT = "DV0003";
    public static final String DUPLICATE_DECLARATION = "DV0004";
    public static final String UNRESOLVED_REFERENCE = "DV0005";
    public static final String FIELD_NOT_IN_DOMAIN = "DV0006";
    public static final String SECRET_FIELD_IN_QUERY = "DV0007";
    public static final String UNKNOWN_UI_KIND = "DV0008";

    private DiagnosticCodes() {
    }
}
