package jlox;

import java.util.List;
import java.util.function.Supplier;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        return parseBinaryLeftAssociative(this::conditional, TokenType.COMMA);
    }

    private Expr conditional() {
        Expr expr = equality();
        if (match(TokenType.QUESTION_MARK)) {
            final Expr thenBranch = expression();
            consume(TokenType.COLON, "Expect ':' after then branch of conditional expression.");
            final Expr elseBranch = conditional();
            expr = new Expr.Ternary(expr, thenBranch, elseBranch);
        }
        return expr;
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
        Jlox.error(token, message);
        return new ParseError();
    }

    // To be used later when we have statements.
    // private void synchronize() {
    //     advance();
    //     while (!isAtEnd()) {
    //         if (previous().type == TokenType.SEMICOLON) {
    //             return;
    //         }
    //         switch (peek().type) {
    //             case TokenType.CLASS:
    //             case TokenType.FUN:
    //             case TokenType.VAR:
    //             case TokenType.FOR:
    //             case TokenType.IF:
    //             case TokenType.WHILE:
    //             case TokenType.PRINT:
    //             case TokenType.RETURN:
    //                 return;
    //             default:
    //                 break;
    //         }
    //         advance();
    //     }
    // }

    private static class ParseError extends RuntimeException {}
}
