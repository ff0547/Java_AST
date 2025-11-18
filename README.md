# JAVA_AST
>  本项目使用 ANTLR4 的官方 Java 语法（JavaLexer.g4 / JavaParser.g4），在其 ParseTree 之上构建了一个自定义的 Java 抽象语法树（AST）。
AST 使用 AstNode 统一表示，包含 kind（节点类型）、text（源代码片段）和有序子节点列表。

## 通过运行 com.myparser.AntlrAstBuilder，可以对指定的 Java 源文件进行解析并打印 AST 结构，用于后续的代码分析或转换。

---
  ### 节点清单
1. 顶层结构

| 节点类型                  | 说明                                                 |
| --------------------- | -------------------------------------------------- |
| `COMPILATION_UNIT`    | 整个 `.java` 源文件的根节点                                 |
| `PACKAGE_DECLARATION` | `package ...;` 声明                                  |
| `IMPORT_DECLARATION`  | `import ...;` 或 `import static ...;`               |
| `CLASS_DECLARATION`   | `class / interface / enum / Inner class` 的声明       |
| `BLOCK`               | 花括号 `{ ... }` 包裹的语句块（类体 / 方法体 / if 块 / finally 块等） |

 - 辅助节点

| 节点类型             | 说明                                        |
| ---------------- | ----------------------------------------- |
| `QUALIFIED_NAME` | 限定名，如 `com.myparser`                      |
| `IDENTIFIER`     | 标识符，如类名、变量名、方法名                           |
| `MODIFIER`       | `public / private / static / final / ...` |

2. 声明相关

| 节点类型                         | 说明                                             |
| ---------------------------- | ---------------------------------------------- |
| `FIELD_DECLARATION`          | 成员变量声明                                         |
| `LOCAL_VARIABLE_DECLARATION` | 局部变量声明                                         |
| `VARIABLE_DECLARATOR`        | 具体的一个变量及其可选初始值                                 |
| `TYPE`                       | 类型名，如 `int`, `String`, `List<String>`, `<T>` 等 |
| `PARAMETER_LIST`             | 形参列表（方法 / 构造函数 / lambda）                       |
| `PARAMETER`                  | 单个形参                                           |
| `CONSTRUCTOR_DECLARATION`    | 构造函数                                           |
| `METHOD_DECLARATION`         | 普通方法 / 静态方法 / 泛型方法                             |

3. 语句

| 节点类型                 | 说明                                                |
| -------------------- | ------------------------------------------------- |
| `EXPR_STATEMENT`     | 以表达式构成的语句（赋值、方法调用等，末尾带 `;`）                       |
| `IF_STATEMENT`       | `if / else if / else` 结构                          |
| `WHILE_STATEMENT`    | `while (...) { ... }`                             |
| `DO_WHILE_STATEMENT` | `do { ... } while (...);`                         |
| `FOR_STATEMENT`      | 普通 `for` / 增强 `for`，通过子节点区分                       |
| `SWITCH_STATEMENT`   | `switch (...) { case ... }`                       |
| `TRY_STATEMENT`      | `try { ... } catch (...) { ... } finally { ... }` |
| `CATCH_CLAUSE`       | 一个 `catch (...) { ... }` 子句                       |
| `FINALLY_BLOCK`      | `finally { ... }` 子句                              |
| `RETURN_STATEMENT`   | `return expr;`                                    |
| `BREAK_STATEMENT`    | `break;`                                          |

4。 表达式

| 节点类型                   | 说明                                       |
| ---------------------- | ---------------------------------------- |
| `ASSIGNMENT_EXPR`      | 赋值表达式：左值 + 右值                            |
| `BINARY_EXPR`          | 二元运算：`+ - * / > < >= <= == !=` 等         |
| `UNARY_EXPR`           | 一元运算：`++ -- !` 等                         |
| `LITERAL`              | 字面量：`0`, `1`, `"str"`, `true`, `false` 等 |
| `MEMBER_SELECT_EXPR`   | 成员访问：`a.b`，链式的通过嵌套实现                     |
| `METHOD_CALL_EXPR`     | 方法调用：方法名 + 实参列表                          |
| `OBJECT_CREATION_EXPR` | `new` 表达式，新建对象                           |
| `EXPRESSION`           | 通用表达式节点，目前主要用于 lambda 根节点                |

| 节点类型（标签）           | 说明                      |
| ------------------ | ----------------------- |
| `TYPE(creator)`    | `new` 后面的类型，用于区分对象创建    |
| `EXPRESSION(args)` | 实参列表的容器                 |
| `EXPRESSION(list)` | 参数列表里的一组表达式（你用它做了一个小包装） |

5. Lambda 相关

| 节点类型           | 说明                      |
| ------------------ | ----------------------- |
| `ExpressionLambda`    | 外层表达式分支   |
| `LambdaExpression` | EXPRESSION(lambda) 节点                 |
| `LambdaParameters` | PARAMETER_LIST |
| `LambdaBody` | 表达式或语句块 |

