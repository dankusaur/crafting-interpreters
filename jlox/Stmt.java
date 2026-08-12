package jlox;

import java.util.List;

abstract class Stmt {

    abstract <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitBlock(Block stmt);
        R visitExpression(Expression stmt);
        R visitIf(If stmt);
        R visitWhile(While stmt);
        R visitPrint(Print stmt);
        R visitVarStmt(VarStmt stmt);
    }

    static class Block extends Stmt {
        final List<Stmt> statements;

        Block(final List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitBlock(this);
        }
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

    static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;

        If(final Expr condition, final Stmt thenBranch, final Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitIf(this);
        }
    }

    static class While extends Stmt {
        final Expr condition;
        final Stmt body;

        While(final Expr condition, final Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        <R> R accept(final Visitor<R> visitor) {
            return visitor.visitWhile(this);
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
