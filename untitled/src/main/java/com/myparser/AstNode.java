package com.myparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一的 AST 节点定义，模仿 parser.y 里各种 new_xxx_statement/new_xxx_expression 的风格。
 */
public class AstNode {

    public enum Kind {
        // 根级别
        COMPILATION_UNIT,
        PACKAGE_DECLARATION,
        IMPORT_DECLARATION,

        // 类型定义
        TYPE_DECLARATION,
        CLASS_DECLARATION,
        INTERFACE_DECLARATION,
        ENUM_DECLARATION,
        RECORD_DECLARATION,

        // 成员
        FIELD_DECLARATION,
        METHOD_DECLARATION,
        CONSTRUCTOR_DECLARATION,
        PARAMETER,
        PARAMETER_LIST,
        LOCAL_VARIABLE_DECLARATION,
        VARIABLE_DECLARATOR,

        // 语句块
        BLOCK,
        BLOCK_STATEMENT,

        // 语句
        IF_STATEMENT,
        FOR_STATEMENT,
        ENHANCED_FOR_STATEMENT,
        WHILE_STATEMENT,
        DO_WHILE_STATEMENT,
        SWITCH_STATEMENT,
        SWITCH_LABEL,
        SWITCH_RULE,
        TRY_STATEMENT,
        CATCH_CLAUSE,
        FINALLY_BLOCK,
        SYNCHRONIZED_STATEMENT,
        RETURN_STATEMENT,
        THROW_STATEMENT,
        BREAK_STATEMENT,
        CONTINUE_STATEMENT,
        YIELD_STATEMENT,
        ASSERT_STATEMENT,
        LABELED_STATEMENT,
        EMPTY_STATEMENT,
        EXPR_STATEMENT,

        // 表达式
        EXPRESSION,
        ASSIGNMENT_EXPR,
        BINARY_EXPR,
        UNARY_EXPR,
        CONDITIONAL_EXPR,
        LAMBDA_EXPR,
        METHOD_CALL_EXPR,
        MEMBER_SELECT_EXPR,
        ARRAY_ACCESS_EXPR,
        OBJECT_CREATION_EXPR,
        ARRAY_CREATION_EXPR,
        CAST_EXPR,
        INSTANCEOF_EXPR,
        PRIMARY_EXPR,

        // 名字 / 类型 / literal
        IDENTIFIER,
        QUALIFIED_NAME,
        TYPE,
        LITERAL,

        // 其它
        MODIFIER,
        ANNOTATION,

        UNKNOWN
    }

    private final Kind kind;
    private String text; // 类名/方法名/操作符/字面量文本等
    private final List<AstNode> children = new ArrayList<>();

    public AstNode(Kind kind) {
        this(kind, null);
    }

    public AstNode(Kind kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    public Kind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<AstNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(AstNode child) {
        if (child != null) {
            children.add(child);
        }
    }

    public void addChildFirst(AstNode child) {
        if (child != null) {
            children.add(0, child);
        }
    }

    @Override
    public String toString() {
        if (text == null) {
            return kind.name();
        }
        return kind.name() + "(" + text + ")";
    }
}
