package jlox.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    private static final String PACKAGE = "jlox";
    private static final String BASE_EXPRESSION_CLASS_NAME = "Expr";
    private static final String BASE_STATEMENT_CLASS_NAME = "Stmt";

    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        final String outputDir = args[0];
        defineAst(outputDir, BASE_EXPRESSION_CLASS_NAME, Arrays.asList(
                "Ternary: Expr condition, Expr thenBranch, Expr elseBranch",
                "Binary     : Expr left, Token operator, Expr right",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Unary      : Token operator, Expr right",
                "Variable   : Token name"
        ));
        defineAst(outputDir, BASE_STATEMENT_CLASS_NAME, Arrays.asList(
            "Expression: Expr expression",
            "Print          : Expr expression",
            "Var            : Token var, Expr initializer"
        ));
    }

    private static void defineAst(final String outputDir, final String baseName, final List<String> types)
            throws FileNotFoundException, UnsupportedEncodingException {
        final String outputPath = outputDir + "/" + baseName + ".java";

        final PrintWriter writer = new PrintWriter(outputPath, StandardCharsets.UTF_8.name());
        writer.println("package " + PACKAGE + ";");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        writer.println();
        writeIndented(writer, "abstract <R> R accept(Visitor<R> visitor);", 1);
        writer.println();

        defineVisitor(writer, baseName, types);

        for (final String type: types) {
            writer.println();
            final String typeName = type.split(":")[0].trim();
            final String fields = type.split(":")[1].trim();
            defineType(writer, baseName, typeName, fields);
        }
        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(final PrintWriter writer, final String baseName, final List<String> types) {
        writeIndented(writer, "interface Visitor<R> {", 1);
        for (final String type: types) {
            final String typeName = type.split(":")[0].trim();
            writeIndented(writer, "R visit" + typeName + "(" + typeName + " " + baseName.toLowerCase() + ");", 2);
        }
        writeIndented(writer, "}", 1);
    }

    private static void defineType(final PrintWriter writer, final String baseName, final String typeName, final String fields) {
        final List<String> separatedFields = Arrays.stream(fields.split(","))
            .map(String::trim).toList();
        final List<String> fieldNames = separatedFields.stream().map(field -> field.split(" ")[1]).toList();
        writeIndented(writer, "static class " + typeName + " extends " + baseName + " {", 1);
        for (final String field: separatedFields) {
            writeIndented(writer, "final " + field + ";", 2);
        }
        writer.println();
        final String constructorArgs = String.join(", ",
                separatedFields.stream().map(field -> "final " + field).toList());
        writeIndented(writer, typeName + "(" + constructorArgs + ") {", 2);
        for (String fieldName: fieldNames) {
            writeIndented(writer, "this." + fieldName + " = " + fieldName + ";", 3);
        }
        writeIndented(writer, "}", 2);
        writer.println();
        writeIndented(writer, "@Override", 2);
        writeIndented(writer, "<R> R accept(final Visitor<R> visitor) {", 2);
        writeIndented(writer, "return visitor.visit" + typeName + "(this);", 3);
        writeIndented(writer, "}", 2);
        writeIndented(writer, "}", 1);
    }

    private static void writeIndented(PrintWriter writer, String string, int indentations) {
        String indentation = "";
        for (int i = 0; i < indentations; ++i) {
            indentation += "    ";
        }
        writer.println(indentation + string);
    }
}
