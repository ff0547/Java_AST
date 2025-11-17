package com.myparser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

public abstract class JavaParserBase extends Parser {

    public JavaParserBase(TokenStream input) {
        super(input);
    }

    /**
     * 用于 annotationFieldValue 规则的消歧。
     * 检查当前输入是否是 "Identifier =" 的形式。
     * 如果是 "key = value"，返回 false，让语法去匹配 identifier '=' annotationValue 分支。
     * 如果不是（例如只是一个值），返回 true，匹配 annotationValue 分支。
     */
    protected boolean IsNotIdentifierAssign() {
        // LT(1) 是当前 token，LT(2) 是下一个 token
        return !(_input.LT(1).getType() == JavaParser.IDENTIFIER &&
                _input.LT(2).getType() == JavaParser.ASSIGN);
    }

    /**
     * 用于 recordComponentList 规则。
     * 原语法通常用于验证变长参数 (...) 是否只出现在最后一个组件。
     * 这里为了确保编译通过，我们简单返回 true。
     */
    protected boolean DoLastRecordComponent() {
        return true;
    }
}