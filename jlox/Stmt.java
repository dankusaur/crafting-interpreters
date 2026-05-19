package jlox;

abstract class Stmt {

    abstract <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitExpression(Expression stmt);
        R visitPrint(Print stmt);
        R visitVarStmt(VarStmt stmt);
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

    static class VarStmt extends Stmt {
        final Token var;
        final Expr initializer;

        VarStmt(final Token var, final Expr initializer) {
            this.var = var;
            this.initializer = initializer;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }
}
