package systems.courant.sd.model.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser that converts an expression string into an {@link Expr} AST.
 *
 * <p>Grammar:
 * <pre>
 * expr       = or_expr
 * or_expr    = and_expr ( "or" and_expr )*
 * and_expr   = comparison ( "and" comparison )*
 * comparison = addition ( ("==" | "!=" | "&lt;" | "&lt;=" | "&gt;" | "&gt;=") addition )?
 * addition   = mult ( ("+" | "-") mult )*
 * mult       = power ( ("*" | "/" | "%") power )*
 * power      = unary ( "**" power )?          // right-associative
 * unary      = ("-" | "+" | "not") unary | call
 * call       = primary ( "(" arglist? ")" )?
 * primary    = NUMBER | IDENTIFIER | QUOTED_ID | "(" expr ")"
 *            | "IF" "(" expr "," expr "," expr ")"
 * arglist    = expr ( "," expr )*
 * </pre>
 *
 * <p>Identifiers may be simple ({@code Population}, {@code Birth_Rate}) or
 * backtick-quoted ({@code `Tasks Remaining`}).
 */
public class ExprParser {

    private static final int MAX_DEPTH = 200;

    private final String input;
    private final int trimOffset;
    private int pos;
    private int depth;

    private ExprParser(String input, int trimOffset) {
        this.input = input;
        this.trimOffset = trimOffset;
        this.pos = 0;
    }

    /**
     * Parses the given expression string into an {@link Expr} AST.
     *
     * <p>The reserved identifiers {@code TIME}, {@code DT}, and {@code PI} are always parsed as
     * zero-argument function calls, consistent with standard System Dynamics convention. Model
     * elements must not use these names.
     *
     * @throws ParseException if the input is not a valid expression
     */
    public static Expr parse(String input) {
        if (input == null || input.isBlank()) {
            throw new ParseException("Expression is empty", 0);
        }
        int trimOffset = 0;
        while (trimOffset < input.length() && Character.isWhitespace(input.charAt(trimOffset))) {
            trimOffset++;
        }
        ExprParser parser = new ExprParser(input.trim(), trimOffset);
        Expr result = parser.parseExpr();
        parser.skipWhitespace();
        if (parser.pos < parser.input.length()) {
            throw new ParseException(
                    "Unexpected character '" + parser.input.charAt(parser.pos) + "'",
                    parser.pos + parser.trimOffset);
        }
        return result;
    }

    private Expr parseExpr() {
        depth++;
        if (depth > MAX_DEPTH) {
            throw new ParseException("Expression nesting too deep (max " + MAX_DEPTH + ")",
                    pos + trimOffset);
        }
        try {
            return parseOr();
        } finally {
            depth--;
        }
    }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (matchKeyword("or")) {
            Expr right = parseAnd();
            left = new Expr.BinaryOp(left, BinaryOperator.OR, right);
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseComparison();
        while (matchKeyword("and")) {
            Expr right = parseComparison();
            left = new Expr.BinaryOp(left, BinaryOperator.AND, right);
        }
        return left;
    }

    private Expr parseComparison() {
        Expr left = parseAddition();
        BinaryOperator op = matchComparisonOp();
        if (op != null) {
            Expr right = parseAddition();
            left = new Expr.BinaryOp(left, op, right);
        }
        return left;
    }

    private BinaryOperator matchComparisonOp() {
        if (match("==")) {
            return BinaryOperator.EQ;
        }
        if (match("!=")) {
            return BinaryOperator.NE;
        }
        if (match("<=")) {
            return BinaryOperator.LE;
        }
        if (match(">=")) {
            return BinaryOperator.GE;
        }
        if (match("<")) {
            return BinaryOperator.LT;
        }
        if (match(">")) {
            return BinaryOperator.GT;
        }
        if (match("=")) {
            return BinaryOperator.EQ;
        }
        return null;
    }

    private Expr parseAddition() {
        Expr left = parseMultiplication();
        while (true) {
            if (match("+")) {
                Expr right = parseMultiplication();
                left = new Expr.BinaryOp(left, BinaryOperator.ADD, right);
            } else if (matchMinus()) {
                Expr right = parseMultiplication();
                left = new Expr.BinaryOp(left, BinaryOperator.SUB, right);
            } else {
                break;
            }
        }
        return left;
    }

    private Expr parseMultiplication() {
        Expr left = parsePower();
        while (true) {
            if (matchSingleStar()) {
                Expr right = parsePower();
                left = new Expr.BinaryOp(left, BinaryOperator.MUL, right);
            } else if (match("/")) {
                Expr right = parsePower();
                left = new Expr.BinaryOp(left, BinaryOperator.DIV, right);
            } else if (match("%")) {
                Expr right = parsePower();
                left = new Expr.BinaryOp(left, BinaryOperator.MOD, right);
            } else {
                break;
            }
        }
        return left;
    }

    private Expr parsePower() {
        Expr base = parseUnary();
        if (match("**")) {
            Expr exponent = parsePower(); // right-associative recursion
            return new Expr.BinaryOp(base, BinaryOperator.POW, exponent);
        }
        return base;
    }

    private Expr parseUnary() {
        if (matchKeyword("not")) {
            depth++;
            if (depth > MAX_DEPTH) {
                throw new ParseException("Expression nesting too deep (max " + MAX_DEPTH + ")",
                        pos + trimOffset);
            }
            Expr operand = parseUnary();
            depth--;
            return new Expr.UnaryOp(UnaryOperator.NOT, operand);
        }
        if (matchMinus()) {
            depth++;
            if (depth > MAX_DEPTH) {
                throw new ParseException("Expression nesting too deep (max " + MAX_DEPTH + ")",
                        pos + trimOffset);
            }
            Expr operand = parseUnary();
            depth--;
            return new Expr.UnaryOp(UnaryOperator.NEGATE, operand);
        }
        if (matchPlus()) {
            // Unary plus is a no-op — just parse the operand
            depth++;
            if (depth > MAX_DEPTH) {
                throw new ParseException("Expression nesting too deep (max " + MAX_DEPTH + ")",
                        pos + trimOffset);
            }
            Expr operand = parseUnary();
            depth--;
            return operand;
        }
        return parsePrimary();
    }

    private Expr parsePrimary() {
        skipWhitespace();
        if (pos >= input.length()) {
            throw new ParseException("Unexpected end of expression", pos + trimOffset);
        }

        char c = input.charAt(pos);

        // Number literal
        if (Character.isDigit(c) || (c == '.' && pos + 1 < input.length()
                && Character.isDigit(input.charAt(pos + 1)))) {
            return parseNumber();
        }

        // Quoted identifier
        if (c == '`') {
            return parseQuotedIdentifier();
        }

        // Parenthesized expression
        if (c == '(') {
            pos++;
            Expr inner = parseExpr();
            expectChar(')');
            return inner;
        }

        // Identifier or function call
        if (isIdentifierStart(c)) {
            return parseIdentifierOrCall();
        }

        throw new ParseException("Unexpected character '" + c + "'", pos + trimOffset);
    }

    private Expr parseNumber() {
        int start = pos;
        // Integer part
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        // Decimal part
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }
        // Scientific notation
        if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
            pos++;
            if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                pos++;
            }
            if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) {
                throw new ParseException("Invalid scientific notation", start + trimOffset);
            }
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }
        String numStr = input.substring(start, pos);
        try {
            return new Expr.Literal(Double.parseDouble(numStr));
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid number: " + numStr, start + trimOffset, e);
        }
    }

    private Expr parseQuotedIdentifier() {
        pos++; // skip opening backtick
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '`') {
            pos++;
        }
        if (pos >= input.length()) {
            throw new ParseException("Unterminated quoted identifier", start - 1 + trimOffset);
        }
        String name = input.substring(start, pos);
        pos++; // skip closing backtick
        if (name.isEmpty()) {
            throw new ParseException("Empty quoted identifier", start - 1 + trimOffset);
        }
        return new Expr.Ref(name);
    }

    private Expr parseIdentifierOrCall() {
        int start = pos;
        while (pos < input.length() && isIdentifierPart(input.charAt(pos))) {
            pos++;
        }
        String name = input.substring(start, pos);

        // Subscript bracket notation: Name[Label]
        if (pos < input.length() && input.charAt(pos) == '[') {
            int bracketStart = pos;
            pos++; // skip '['
            int labelStart = pos;
            while (pos < input.length() && input.charAt(pos) != ']') {
                pos++;
            }
            if (pos >= input.length()) {
                throw new ParseException("Unterminated subscript bracket", bracketStart + trimOffset);
            }
            String label = input.substring(labelStart, pos);
            pos++; // skip ']'
            if (label.isEmpty()) {
                throw new ParseException("Empty subscript label", bracketStart + trimOffset);
            }
            name = name + "[" + label + "]";
        }

        skipWhitespace();

        // IF and IF_SHORT are special: IF(condition, then, else) / IF_SHORT(condition, then, else)
        if ((name.equals("IF") || name.equals("IF_SHORT"))
                && pos < input.length() && input.charAt(pos) == '(') {
            boolean shortCircuit = name.equals("IF_SHORT");
            pos++; // skip '('
            Expr condition = parseExpr();
            expectChar(',');
            Expr thenExpr = parseExpr();
            expectChar(',');
            Expr elseExpr = parseExpr();
            expectChar(')');
            return new Expr.Conditional(condition, thenExpr, elseExpr, shortCircuit);
        }

        // Function call: NAME(args)
        if (pos < input.length() && input.charAt(pos) == '(') {
            pos++; // skip '('
            List<Expr> args = new ArrayList<>();
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) != ')') {
                args.add(parseExpr());
                while (matchChar(',')) {
                    args.add(parseExpr());
                }
            }
            expectChar(')');
            return new Expr.FunctionCall(name, args);
        }

        // Zero-arg function calls (TIME, DT, PI)
        if (name.equals("TIME") || name.equals("DT") || name.equals("PI")) {
            return new Expr.FunctionCall(name, List.of());
        }

        // Plain reference
        return new Expr.Ref(name);
    }

    // --- Utility methods ---

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private boolean match(String expected) {
        skipWhitespace();
        if (input.startsWith(expected, pos)) {
            // For operators like "<", make sure we don't match "<=" when looking for "<"
            if (expected.length() == 1) {
                char ch = expected.charAt(0);
                if ((ch == '<' || ch == '>') && pos + 1 < input.length()
                        && input.charAt(pos + 1) == '=') {
                    return false;
                }
            }
            pos += expected.length();
            return true;
        }
        return false;
    }

    /**
     * Matches a single '*' that is NOT part of '**' (exponentiation).
     */
    private boolean matchSingleStar() {
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == '*') {
            if (pos + 1 < input.length() && input.charAt(pos + 1) == '*') {
                return false; // this is '**', not '*'
            }
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Matches a keyword (like "and", "or", "not") followed by a non-identifier character.
     * Case-insensitive. This prevents matching "and" inside "android".
     */
    private boolean matchKeyword(String keyword) {
        skipWhitespace();
        int end = pos + keyword.length();
        if (end > input.length()) {
            return false;
        }
        if (!input.substring(pos, end).equalsIgnoreCase(keyword)) {
            return false;
        }
        // Must not be followed by an identifier character
        if (end < input.length() && isIdentifierPart(input.charAt(end))) {
            return false;
        }
        pos = end;
        return true;
    }

    /**
     * Matches a unary '+' (no-op sign). Only called from parseUnary.
     */
    private boolean matchPlus() {
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == '+') {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Matches a '-' that should be treated as a binary subtraction operator,
     * not a unary minus. Called only from parseAddition where we know the left
     * operand has already been parsed.
     */
    private boolean matchMinus() {
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == '-') {
            // Don't match if it's actually part of a number or identifier following
            // (that's handled by unary minus). But in addition context, '-' is subtraction.
            pos++;
            return true;
        }
        return false;
    }

    private boolean matchChar(char c) {
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void expectChar(char c) {
        skipWhitespace();
        if (pos >= input.length() || input.charAt(pos) != c) {
            throw new ParseException("Expected '" + c + "'", pos + trimOffset);
        }
        pos++;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
