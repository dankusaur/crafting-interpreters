package jlox;

abstract class Stmt {

    abstract <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitExpression(Expression stmt);
        R visitPrint(Print stmt);
    }

    static class Expression extends Stmt {
        final Expr expression;

        Expression(final Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitExpression(this);
        }
    }

    static class Print extends Stmt {
        final Expr expression;

        Print(final Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitPrint(this);
        }
    }
}
