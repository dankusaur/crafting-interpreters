package jlox.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    private static final String PACKAGE = "jlox";
    private static final String BASE_EXPRESSION_CLASS_NAME = "Expr";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, BASE_EXPRESSION_CLASS_NAME, Arrays.asList(
                "Binary: Expr left, Token operator, Expr right",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Unary      : Token operator, Expr right"
        ));
    }
    private static void defineAst(String outputDir, String baseName, List<String> types) throws FileNotFoundException, UnsupportedEncodingException {
        String outputPath = outputDir + "/" + baseName + ".java";

        PrintWriter writer = new PrintWriter(outputPath, "UTF-8");
        writer.println("package " + PACKAGE + ";");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.write("abstract class " + baseName + " {");
        for (String type: types) {
            writer.println();
            String typeName = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, typeName, fields);
        }
        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName, String typeName, String fields) {
        List<String> separatedFields = Arrays.stream(fields.split(","))
            .map(String::trim).toList();
        List<String> fieldNames = separatedFields.stream().map(field -> field.split(" ")[1]).toList();
        writeIndented(writer, "static class " + typeName + " extends " + baseName + " {", 1);
        for (String field: separatedFields) {
            writeIndented(writer, "final " + field + ";", 2);
        }
        writer.println();
        writeIndented(writer, typeName + "(" + fields + ") {", 2);
        for (String fieldName: fieldNames) {
            writeIndented(writer, "this." + fieldName + " = " + fieldName + ";", 3);
        }
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
