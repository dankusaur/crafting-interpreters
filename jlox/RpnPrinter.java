package jlox;

import jlox.Expr.Assign;
import jlox.Expr.Binary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Unary;
import jlox.Expr.Variable;

public class RpnPrinter implements Expr.Visitor<String>{

    public static void main(final String[] args) {
        final Expr expression = new Expr.Binary(
                new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));
        System.out.println(new RpnPrinter().print(expression));
    }

    String print(final Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitTernary(final Expr.Ternary expr) {
        return formatRpn("?:", expr.condition, expr.thenBranch, expr.elseBranch);
    }

    @Override
    public String visitBinary(final Binary expr) {
        return formatRpn(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGrouping(final Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visitUnary(final Unary expr) {
        final String lexeme;
        if (expr.operator.type == TokenType.MINUS) {
            lexeme = "~";
        } else {
            lexeme = expr.operator.lexeme;
        }
        return formatRpn(lexeme, expr.right);
    }

    @Override
    public String visitVariable(Variable expr) {
        return expr.name.lexeme + "ref";
    }

    @Override
    public String visitLiteral(final Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    private String formatRpn(final String lexeme, final Expr ...exprs) {
        StringBuilder stringBuilder = new StringBuilder();
        for (final Expr expr: exprs) {
            stringBuilder.append(expr.accept(this));
            stringBuilder.append(' ');
        }
        stringBuilder.append(lexeme);

        return stringBuilder.toString();
    }

    @Override
    public String visitAssign(Assign expr) {
        return formatRpn(expr.name.lexeme, expr.value);
    }
}
