package com.myparser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 入口类：读取一个 Java 源文件，构建 ANTLR 语法树 + Java AST，并打印 AST。
 */
public class AntlrAstBuilder {

    public static void main(String[] args) throws IOException {

        // 1. 要解析的 Java 源码路径
        String inputPath;
        if (args.length > 0) {
            // 命令行方式：
            //   java -cp target/classes;antlr-4.13.2-complete.jar com.myparser.AntlrAstBuilder path/to/File.java
            inputPath = args[0];
        } else {
            // IntelliJ 直接运行时，默认解析这个文件；你可以改成自己的测试文件
            inputPath = Paths.get("src", "main", "java", "com", "myparser", "MyTestFile.java").toString();
        }

        System.out.println("Parsing file: " + inputPath);

        // 2. 词法 + 语法分析
        CharStream input = CharStreams.fromFileName(inputPath);
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        // 3. 生成 ParseTree（分析树）
        JavaParser.CompilationUnitContext tree = parser.compilationUnit();

        // 4. 用自定义 Visitor 构造 AST
        JavaAstBuilderVisitor visitor = new JavaAstBuilderVisitor();
        AstNode astRoot = visitor.visitCompilationUnit(tree);

        // 5. 打印 AST
        System.out.println("parse is OK");
        AstPrinter.print(astRoot);
    }
}
