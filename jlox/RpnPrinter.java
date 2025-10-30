package jlox;

import jlox.Expr.Binary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Unary;

public class RpnPrinter implements Expr.Visitor<String>{

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));
        System.out.println(new RpnPrinter().print(expression));
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitTernary(Expr.Ternary expr) {
        return formatRpn("?:", expr.condition, expr.thenBranch, expr.elseBranch);
    }

    @Override
    public String visitBinary(Binary expr) {
        return formatRpn(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGrouping(Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visitLiteral(Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitUnary(Unary expr) {
        String lexeme;
        if (expr.operator.type == TokenType.MINUS) {
            lexeme = "~";
        } else {
            lexeme = expr.operator.lexeme;
        }
        return formatRpn(lexeme, expr.right);
    }

    private String formatRpn(String lexeme, Expr ...exprs) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Expr expr: exprs) {
            stringBuilder.append(expr.accept(this));
            stringBuilder.append(' ');
        }
        stringBuilder.append(lexeme);

        return stringBuilder.toString();
    }
}
