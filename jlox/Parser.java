package jlox;

import static jlox.TokenType.ELSE;
import static jlox.TokenType.EQUAL;
import static jlox.TokenType.IDENTIFIER;
import static jlox.TokenType.IF;
import static jlox.TokenType.LEFT_BRACE;
import static jlox.TokenType.LEFT_PAREN;
import static jlox.TokenType.PRINT;
import static jlox.TokenType.RIGHT_BRACE;
import static jlox.TokenType.RIGHT_PAREN;
import static jlox.TokenType.SEMICOLON;
import static jlox.TokenType.VAR;
import static jlox.TokenType.WHILE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    private final ErrorReporter errorReporter;

    Parser(final List<Token> tokens, final ErrorReporter errorReporter) {
        this.tokens = tokens;
        this.errorReporter = errorReporter;
    }

    List<Stmt> parse() {
        final List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    Expr parseExpression() {
        try {
            return comma();
        } catch (final ParseError error) {
            return null;
        }
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (final ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        final Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.VarStmt(name, initializer);
    }

    private Stmt statement() {
        if (match(IF)) {
            return ifStatement();
        }
        if (match(PRINT)) {
            return printStatement();
        }
        if (match (LEFT_BRACE)) {
            return blockStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after if.");
        final Expr expr = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");
        final Stmt thenBranch = statement();
        final Stmt elseBranch;
        if (match(ELSE)) {
            elseBranch = statement();
        } else {
            elseBranch = null;
        }
        return new Stmt.If(expr, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        final Expr expr = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'while' condition.");
        final Stmt body = statement();
        return new Stmt.While(expr, body);
    }

    private Stmt printStatement() {
        final Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(expr);
    }

    private Stmt blockStatement() {
        final List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return new Stmt.Block(statements);
    }

    private Stmt expressionStatement() {
        final Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        return parseBinaryLeftAssociative(this::assignment, TokenType.COMMA);
    }

    private Expr assignment() {
        Expr expr = conditional();
        if (match(EQUAL)) {
            final Token equals = previous();
            final Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                final Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr conditional() {
        Expr expr = or();
        if (match(TokenType.QUESTION_MARK)) {
            final Expr thenBranch = expression();
            consume(TokenType.COLON, "Expect ':' after then branch of conditional expression.");
            final Expr elseBranch = conditional();
            expr = new Expr.Ternary(expr, thenBranch, elseBranch);
        }
        return expr;
    }

    private Expr or() {
        return parseLogicalLeftAssociative(this::and, TokenType.OR);
    }

    private Expr and() {
        return parseLogicalLeftAssociative(this::equality, TokenType.AND);
    }

    private Expr equality() {
        return parseBinaryLeftAssociative(this::comparison, TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL);
    }

    private Expr comparison() {
        return parseBinaryLeftAssociative(this::term, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER,
                TokenType.GREATER_EQUAL);
    }

    private Expr term() {
        return parseBinaryLeftAssociative(this::factor, TokenType.PLUS, TokenType.MINUS);
    }

    private Expr factor() {
        return parseBinaryLeftAssociative(this::unary, TokenType.STAR, TokenType.SLASH);
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            final Token operator = previous();
            final Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            error(previous(), "Missing left-hand operand for equality.");
            equality();
            return null;
        }
        if (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            error(previous(), "Missing left-hand operand for comparison.");
            comparison();
            return null;
        }
        if (match(TokenType.PLUS)) {
            error(previous(), "Missing left-hand operand for term.");
            term();
            return null;
        }
        if (match(TokenType.SLASH, TokenType.STAR)) {
            error(previous(), "Missing left-hand operand for factor.");
            factor();
            return null;
        }

        throw error(peek(), "Expect expression.");
    }

    private Expr parseLogicalLeftAssociative(final Supplier<Expr> operandParser, final TokenType... operators) {
        Expr expr = operandParser.get();
        while (match(operators)) {
            final Token operator = previous();
            final Expr right = operandParser.get();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr parseBinaryLeftAssociative(final Supplier<Expr> operandParser, final TokenType... operators) {
        Expr expr = operandParser.get();
        while (match(operators)) {
            final Token operator = previous();
            final Expr right = operandParser.get();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private boolean match(TokenType... types) {
        if (isAtEnd()) {
            return false;
        }
        for (final TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(final TokenType type) {
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            ++current;
        }
        return previous();
    }

    private Token consume(final TokenType type, final String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(final Token token, final String message) {
        errorReporter.syntaxError(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return;
            }
            switch (peek().type) {
                case TokenType.CLASS:
                case TokenType.FUN:
                case TokenType.VAR:
                case TokenType.FOR:
                case TokenType.IF:
                case TokenType.WHILE:
                case TokenType.PRINT:
                case TokenType.RETURN:
                    return;
                default:
                    break;
            }
            advance();
        }
    }

    private static class ParseError extends RuntimeException {}
}
