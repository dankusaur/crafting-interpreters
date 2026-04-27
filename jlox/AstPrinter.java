package jlox;

import java.util.List;

import jlox.Expr.Variable;
import jlox.Stmt.Expression;
import jlox.Stmt.Print;
import jlox.Stmt.Var;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    public static void main(final String[] args) {
        final Expr expression = new Expr.Binary(
                new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));
        new AstPrinter().print(List.of(new Stmt.Expression(expression)));
    }

    Void print(final List<Stmt> statements) {
        for (final Stmt statement: statements) {
            System.out.println(statement.accept(this));
        }
        return null;
    }

    @Override
    public String visitVar(Var stmt) {
        return "─ var " + stmt.var + "=" + stmt.initializer.accept(this);
    }

    @Override
    public String visitExpression(Expression stmt) {
        return "─ "+ stmt.expression.accept(this);
    }

    @Override
    public String visitPrint(Print stmt) {
        final StringBuilder repr = new StringBuilder();
        repr.append("─ print ");
        repr.append(stmt.expression.accept(this));
        return repr.toString();
    }

    @Override
    public String visitTernary(final Expr.Ternary expr) {
        return parenthesize("?:", expr.condition, expr.thenBranch, expr.elseBranch);
    }

    @Override
    public String visitBinary(final Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGrouping(final Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitUnary(final Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitLiteral(final Expr.Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitVariable(Variable expr) {
        return expr.name + "=?";
    }

    private String parenthesize(final String lexeme, final Expr... exprs) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('(').append(lexeme);
        for (Expr expr : exprs) {
            stringBuilder.append(' ');
            stringBuilder.append(expr.accept(this));
        }
        stringBuilder.append(')');

        return stringBuilder.toString();
    }
}
