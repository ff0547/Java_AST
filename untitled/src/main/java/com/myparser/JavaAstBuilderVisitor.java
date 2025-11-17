package com.myparser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 ANTLR 生成的 JavaParser + JavaParserBaseVisitor，
 * 按照 parser.y 构建 Java AST。
 */
public class JavaAstBuilderVisitor extends JavaBaseVisitor<AstNode> {

    /* ========= 小工具函数 ========= */

    private AstNode n(AstNode.Kind kind, String text, AstNode... children) {
        AstNode node = new AstNode(kind, text);
        if (children != null) {
            for (AstNode c : children) {
                node.addChild(c);
            }
        }
        return node;
    }

    private AstNode n(AstNode.Kind kind, AstNode... children) {
        return n(kind, null, children);
    }

    private List<AstNode> visitList(List<? extends ParseTree> ctxList) {
        List<AstNode> result = new ArrayList<>();
        for (ParseTree t : ctxList) {
            AstNode c = t.accept(this);
            if (c != null) {
                result.add(c);
            }
        }
        return result;
    }

    /* ========= compilation unit / package / import ========= */

    @Override
    public AstNode visitCompilationUnit(JavaParser.CompilationUnitContext ctx) {
        AstNode root = new AstNode(AstNode.Kind.COMPILATION_UNIT);

        if (ctx.packageDeclaration() != null) {
            root.addChild(visit(ctx.packageDeclaration()));
        }

        for (JavaParser.ImportDeclarationContext ic : ctx.importDeclaration()) {
            root.addChild(visit(ic));
        }

        for (JavaParser.TypeDeclarationContext td : ctx.typeDeclaration()) {
            AstNode t = visit(td);
            if (t != null) {
                root.addChild(t);
            }
        }

        if (ctx.modularCompulationUnit() != null) {
            root.addChild(visit(ctx.modularCompulationUnit()));
        }

        return root;
    }

    @Override
    public AstNode visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        AstNode pkg = new AstNode(AstNode.Kind.PACKAGE_DECLARATION);
        pkg.addChild(visit(ctx.qualifiedName()));
        return pkg;
    }

    @Override
    public AstNode visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        AstNode imp = new AstNode(AstNode.Kind.IMPORT_DECLARATION);
        imp.setText(ctx.getText());
        imp.addChild(visit(ctx.qualifiedName()));
        return imp;
    }

    @Override
    public AstNode visitQualifiedName(JavaParser.QualifiedNameContext ctx) {
        AstNode qn = new AstNode(AstNode.Kind.QUALIFIED_NAME, ctx.getText());
        for (JavaParser.IdentifierContext idCtx : ctx.identifier()) {
            qn.addChild(visit(idCtx));
        }
        return qn;
    }

    /* ========= 类型声明（class/interface/enum/record） ========= */

    @Override
    public AstNode visitTypeDeclaration(JavaParser.TypeDeclarationContext ctx) {
        AstNode typeNode;

        if (ctx.classDeclaration() != null) {
            typeNode = visit(ctx.classDeclaration());
        } else if (ctx.interfaceDeclaration() != null) {
            typeNode = visit(ctx.interfaceDeclaration());
        } else if (ctx.enumDeclaration() != null) {
            typeNode = visit(ctx.enumDeclaration());
        } else if (ctx.annotationTypeDeclaration() != null) {
            // 注解类型，当作特殊的接口
            typeNode = visit(ctx.annotationTypeDeclaration());
        } else if (ctx.recordDeclaration() != null) {
            typeNode = visit(ctx.recordDeclaration());
        } else {
            return visitChildren(ctx);
        }

        // 把修饰符挂到类型节点前面，模仿 parser.y 把属性挂在语句上的风格
        for (JavaParser.ClassOrInterfaceModifierContext m : ctx.classOrInterfaceModifier()) {
            AstNode mod = visit(m);
            if (mod != null) {
                typeNode.addChildFirst(mod);
            }
        }

        return typeNode;
    }

    @Override
    public AstNode visitClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
        if (ctx.annotation() != null) {
            AstNode ann = new AstNode(AstNode.Kind.ANNOTATION, ctx.annotation().getText());
            return ann;
        }
        // PUBLIC / PROTECTED / PRIVATE / ABSTRACT / STATIC / FINAL 等
        String text = ctx.getText();
        return new AstNode(AstNode.Kind.MODIFIER, text);
    }

    @Override
    public AstNode visitModifier(JavaParser.ModifierContext ctx) {
        if (ctx.classOrInterfaceModifier() != null) {
            return visit(ctx.classOrInterfaceModifier());
        }
        return new AstNode(AstNode.Kind.MODIFIER, ctx.getText());
    }

    @Override
    public AstNode visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode clazz = new AstNode(AstNode.Kind.CLASS_DECLARATION, name);

        if (ctx.typeParameters() != null) {
            clazz.addChild(visit(ctx.typeParameters()));
        }

        if (ctx.typeType() != null) {
            AstNode ext = n(AstNode.Kind.TYPE, "extends", visit(ctx.typeType()));
            clazz.addChild(ext);
        }
        int typeListIndex = 0;

        // 处理 implements
        if (ctx.IMPLEMENTS() != null) {
            // 获取当前的 typeList，并将索引 +1
            AstNode impl = n(AstNode.Kind.TYPE, "implements", visit(ctx.typeList(typeListIndex++)));
            clazz.addChild(impl);
        }

        // 处理 permits (Java 17 sealed classes)
        if (ctx.PERMITS() != null) {
            AstNode permits = n(AstNode.Kind.TYPE, "permits", visit(ctx.typeList(typeListIndex++)));
            clazz.addChild(permits);
        }
        clazz.addChild(visit(ctx.classBody()));
        return clazz;
    }

    @Override
    public AstNode visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode iface = new AstNode(AstNode.Kind.INTERFACE_DECLARATION, name);

        if (ctx.typeParameters() != null) {
            iface.addChild(visit(ctx.typeParameters()));
        }
        int typeListIndex = 0;

        // interface 的 extends 使用的是 typeList
        if (ctx.EXTENDS() != null) {
            AstNode ext = n(AstNode.Kind.TYPE, "extends", visit(ctx.typeList(typeListIndex++)));
            iface.addChild(ext);
        }

        if (ctx.PERMITS() != null) {
            AstNode permits = n(AstNode.Kind.TYPE, "permits", visit(ctx.typeList(typeListIndex++)));
            iface.addChild(permits);
        }

        iface.addChild(visit(ctx.interfaceBody()));
        return iface;
    }

    @Override
    public AstNode visitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode en = new AstNode(AstNode.Kind.ENUM_DECLARATION, name);
        en.addChild(visit(ctx.enumBodyDeclarations())); // 里面再由默认规则展开
        return en;
    }

    @Override
    public AstNode visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode rec = new AstNode(AstNode.Kind.RECORD_DECLARATION, name);

        if (ctx.typeParameters() != null) {
            rec.addChild(visit(ctx.typeParameters()));
        }
        if (ctx.recordHeader() != null) {
            rec.addChild(visit(ctx.recordHeader()));
        }
        if (ctx.typeList() != null) {
            AstNode impl = n(AstNode.Kind.TYPE, "implements", visit(ctx.typeList()));
            rec.addChild(impl);
        }
        if (ctx.recordBody() != null) {
            rec.addChild(visit(ctx.recordBody()));
        }

        return rec;
    }

    @Override
    public AstNode visitClassBody(JavaParser.ClassBodyContext ctx) {
        AstNode body = new AstNode(AstNode.Kind.BLOCK, "classBody");
        for (JavaParser.ClassBodyDeclarationContext c : ctx.classBodyDeclaration()) {
            AstNode child = visit(c);
            if (child != null) {
                body.addChild(child);
            }
        }
        return body;
    }

    @Override
    public AstNode visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        if (ctx.SEMI() != null) {
            return null; // 空分号
        }
        if (ctx.block() != null) {
            AstNode block = visit(ctx.block());
            if (ctx.STATIC() != null) {
                AstNode staticBlock = new AstNode(AstNode.Kind.BLOCK, "static");
                staticBlock.addChild(block);
                return staticBlock;
            }
            return block;
        }
        if (ctx.memberDeclaration() != null) {
            AstNode member = visit(ctx.memberDeclaration());
            for (JavaParser.ModifierContext m : ctx.modifier()) {
                AstNode mod = visit(m);
                if (mod != null) {
                    member.addChildFirst(mod);
                }
            }
            return member;
        }
        return visitChildren(ctx);
    }

    @Override
    public AstNode visitMemberDeclaration(JavaParser.MemberDeclarationContext ctx) {
        if (ctx.recordDeclaration() != null) {
            return visit(ctx.recordDeclaration());
        }
        if (ctx.methodDeclaration() != null) {
            return visit(ctx.methodDeclaration());
        }
        if (ctx.genericMethodDeclaration() != null) {
            return visit(ctx.genericMethodDeclaration());
        }
        if (ctx.fieldDeclaration() != null) {
            return visit(ctx.fieldDeclaration());
        }
        if (ctx.constructorDeclaration() != null) {
            return visit(ctx.constructorDeclaration());
        }
        if (ctx.genericConstructorDeclaration() != null) {
            return visit(ctx.genericConstructorDeclaration());
        }
        if (ctx.interfaceDeclaration() != null) {
            return visit(ctx.interfaceDeclaration());
        }
        if (ctx.annotationTypeDeclaration() != null) {
            return visit(ctx.annotationTypeDeclaration());
        }
        if (ctx.classDeclaration() != null) {
            return visit(ctx.classDeclaration());
        }
        if (ctx.enumDeclaration() != null) {
            return visit(ctx.enumDeclaration());
        }
        return visitChildren(ctx);
    }

    /* ========= 字段 / 方法 / 构造函数 ========= */

    @Override
    public AstNode visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        AstNode field = new AstNode(AstNode.Kind.FIELD_DECLARATION);
        field.addChild(visit(ctx.typeType()));
        field.addChild(visit(ctx.variableDeclarators()));
        return field;
    }

    @Override
    public AstNode visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode method = new AstNode(AstNode.Kind.METHOD_DECLARATION, name);

        method.addChild(visit(ctx.typeTypeOrVoid()));
        method.addChild(visit(ctx.formalParameters()));
        if (!ctx.LBRACK().isEmpty()) {
            // 返回类型后的 []，简单记录一下
            method.addChild(new AstNode(AstNode.Kind.TYPE, "arrayReturn"));
        }
        if (ctx.qualifiedNameList() != null) {
            AstNode throwsNode = new AstNode(AstNode.Kind.TYPE, "throws");
            throwsNode.addChild(visit(ctx.qualifiedNameList()));
            method.addChild(throwsNode);
        }
        method.addChild(visit(ctx.methodBody()));
        return method;
    }

    @Override
    public AstNode visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode ctor = new AstNode(AstNode.Kind.CONSTRUCTOR_DECLARATION, name);
        ctor.addChild(visit(ctx.formalParameters()));
        if (ctx.qualifiedNameList() != null) {
            AstNode throwsNode = new AstNode(AstNode.Kind.TYPE, "throws");
            throwsNode.addChild(visit(ctx.qualifiedNameList()));
            ctor.addChild(throwsNode);
        }
        ctor.addChild(visit(ctx.constructorBody));
        return ctor;
    }

    @Override
    public AstNode visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        AstNode params = new AstNode(AstNode.Kind.PARAMETER_LIST);

        if (ctx.receiverParameter() != null) {
            params.addChild(visit(ctx.receiverParameter()));
        }

        // --- 新增：处理第一个参数 ---
        if (ctx.formalParameter() != null) {
            params.addChild(visit(ctx.formalParameter()));
        }
        // ------------------------

        for (JavaParser.FormalParameterListContext listCtx : ctx.formalParameterList()) {
            params.addChild(visit(listCtx));
        }
        return params;
    }


    @Override
    public AstNode visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        AstNode param = new AstNode(AstNode.Kind.PARAMETER);
        if (ctx.typeType() != null) {
            param.addChild(visit(ctx.typeType()));
        }
        param.addChild(visit(ctx.variableDeclaratorId()));
        return param;
    }

    @Override
    public AstNode visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
        AstNode vd = new AstNode(AstNode.Kind.VARIABLE_DECLARATOR);
        vd.addChild(visit(ctx.variableDeclaratorId()));
        if (ctx.variableInitializer() != null) {
            vd.addChild(visit(ctx.variableInitializer()));
        }
        return vd;
    }

    @Override
    public AstNode visitVariableDeclarators(JavaParser.VariableDeclaratorsContext ctx) {
        AstNode list = new AstNode(AstNode.Kind.LOCAL_VARIABLE_DECLARATION);
        for (JavaParser.VariableDeclaratorContext v : ctx.variableDeclarator()) {
            list.addChild(visit(v));
        }
        return list;
    }

    @Override
    public AstNode visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        AstNode local = new AstNode(AstNode.Kind.LOCAL_VARIABLE_DECLARATION);
        if (!ctx.variableModifier().isEmpty()) {
            for (JavaParser.VariableModifierContext m : ctx.variableModifier()) {
                AstNode mod = new AstNode(AstNode.Kind.MODIFIER, m.getText());
                local.addChild(mod);
            }
        }
        if (ctx.VAR() != null) {
            // var identifier = expression
            AstNode type = new AstNode(AstNode.Kind.TYPE, "var");
            local.addChild(type);
            local.addChild(visit(ctx.identifier()));
            local.addChild(visit(ctx.expression()));
        } else {
            local.addChild(visit(ctx.typeType()));
            local.addChild(visit(ctx.variableDeclarators()));
        }
        return local;
    }

    /* ========= 块 / 语句 ========= */

    @Override
    public AstNode visitBlock(JavaParser.BlockContext ctx) {
        AstNode block = new AstNode(AstNode.Kind.BLOCK);
        for (JavaParser.BlockStatementContext b : ctx.blockStatement()) {
            AstNode child = visit(b);
            if (child != null) {
                block.addChild(child);
            }
        }
        return block;
    }

    @Override
    public AstNode visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        if (ctx.localVariableDeclaration() != null) {
            return visit(ctx.localVariableDeclaration());
        }
        if (ctx.localTypeDeclaration() != null) {
            return visit(ctx.localTypeDeclaration());
        }
        if (ctx.statement() != null) {
            return visit(ctx.statement());
        }
        return visitChildren(ctx);
    }

    @Override
    public AstNode visitStatement(JavaParser.StatementContext ctx) {
        // 1) block
        if (ctx.blockLabel != null) {
            return visit(ctx.blockLabel);
        }

        // 2) assert
        if (ctx.ASSERT() != null) {
            AstNode node = new AstNode(AstNode.Kind.ASSERT_STATEMENT);
            node.addChild(visit(ctx.expression(0)));
            if (ctx.expression().size() > 1) {
                node.addChild(visit(ctx.expression(1)));
            }
            return node;
        }

        // 3) if
        if (ctx.IF() != null) {
            AstNode cond = visit(ctx.expression(0));
            AstNode thenStmt = visit(ctx.statement(0));
            AstNode elseStmt = ctx.ELSE() != null && ctx.statement().size() > 1
                    ? visit(ctx.statement(1))
                    : null;
            return n(AstNode.Kind.IF_STATEMENT, cond, thenStmt, elseStmt);
        }

        // 4) for / enhanced for
        if (ctx.FOR() != null) {
            AstNode control = visit(ctx.forControl());
            AstNode body = visit(ctx.statement(0));
            if (ctx.forControl().enhancedForControl() != null) {
                return n(AstNode.Kind.ENHANCED_FOR_STATEMENT, control, body);
            }
            return n(AstNode.Kind.FOR_STATEMENT, control, body);
        }

        // 5) while
        if (ctx.WHILE() != null) {
            AstNode cond = visit(ctx.expression(0));
            AstNode body = visit(ctx.statement(0));
            return n(AstNode.Kind.WHILE_STATEMENT, cond, body);
        }

        // 6) do-while
        if (ctx.DO() != null && ctx.WHILE() != null) {
            AstNode body = visit(ctx.statement(0));
            AstNode cond = visit(ctx.expression(0));
            return n(AstNode.Kind.DO_WHILE_STATEMENT, cond, body);
        }

        // 7) try
        if (ctx.TRY() != null) {
            AstNode tryNode = new AstNode(AstNode.Kind.TRY_STATEMENT);
            if (ctx.block() != null) {
                tryNode.addChild(visit(ctx.block()));
            }
            for (JavaParser.CatchClauseContext cc : ctx.catchClause()) {
                tryNode.addChild(visit(cc));
            }
            if (ctx.finallyBlock() != null) {
                tryNode.addChild(visit(ctx.finallyBlock()));
            }
            if (ctx.resourceSpecification() != null) {
                tryNode.addChild(visit(ctx.resourceSpecification()));
            }
            return tryNode;
        }

        // 8) switch (语句)
        if (ctx.SWITCH() != null) {
            AstNode sw = new AstNode(AstNode.Kind.SWITCH_STATEMENT);
            sw.addChild(visit(ctx.expression(0)));
            for (JavaParser.SwitchBlockStatementGroupContext g : ctx.switchBlockStatementGroup()) {
                sw.addChild(visit(g));
            }
            for (JavaParser.SwitchLabelContext l : ctx.switchLabel()) {
                sw.addChild(visit(l));
            }
            return sw;
        }

        // 9) synchronized
        if (ctx.SYNCHRONIZED() != null) {
            AstNode sync = new AstNode(AstNode.Kind.SYNCHRONIZED_STATEMENT);
            sync.addChild(visit(ctx.expression(0)));
            sync.addChild(visit(ctx.block()));
            return sync;
        }

        // 10) return
        if (ctx.RETURN() != null) {
            AstNode ret = new AstNode(AstNode.Kind.RETURN_STATEMENT);
            if (!ctx.expression().isEmpty()) {
                ret.addChild(visit(ctx.expression(0)));
            }
            return ret;
        }

        // 11) throw
        if (ctx.THROW() != null) {
            AstNode t = new AstNode(AstNode.Kind.THROW_STATEMENT);
            t.addChild(visit(ctx.expression(0)));
            return t;
        }

        // 12) break
        if (ctx.BREAK() != null) {
            AstNode b = new AstNode(AstNode.Kind.BREAK_STATEMENT);
            if (ctx.identifier() != null) {
                b.addChild(visit(ctx.identifier()));
            }
            return b;
        }

        // 13) continue
        if (ctx.CONTINUE() != null) {
            AstNode c = new AstNode(AstNode.Kind.CONTINUE_STATEMENT);
            if (ctx.identifier() != null) {
                c.addChild(visit(ctx.identifier()));
            }
            return c;
        }

        // 14) yield
        if (ctx.YIELD() != null) {
            AstNode y = new AstNode(AstNode.Kind.YIELD_STATEMENT);
            y.addChild(visit(ctx.expression(0)));
            return y;
        }

        // 16) 以表达式结尾的语句 (Assignment, Method Call 等)
        if (ctx.statementExpression != null || ctx.switchExpression() != null) {
            AstNode expr = ctx.statementExpression != null
                    ? visit(ctx.statementExpression)
                    : visit(ctx.switchExpression());
            return n(AstNode.Kind.EXPR_STATEMENT, expr);
        }

        // 15) 空分号 (放在表达式语句判断之后)
        if (ctx.SEMI() != null) {
            return new AstNode(AstNode.Kind.EMPTY_STATEMENT);
        }

        // 17) 带 label 的语句
        if (ctx.identifierLabel != null) {
            AstNode label = new AstNode(AstNode.Kind.LABELED_STATEMENT, ctx.identifierLabel.getText());
            label.addChild(visit(ctx.statement(0)));
            return label;
        }

        return visitChildren(ctx);
    }

    @Override
    public AstNode visitCatchClause(JavaParser.CatchClauseContext ctx) {
        AstNode catchNode = new AstNode(AstNode.Kind.CATCH_CLAUSE);
        catchNode.addChild(visit(ctx.catchType()));
        catchNode.addChild(visit(ctx.identifier()));
        catchNode.addChild(visit(ctx.block()));
        return catchNode;
    }

    @Override
    public AstNode visitFinallyBlock(JavaParser.FinallyBlockContext ctx) {
        AstNode f = new AstNode(AstNode.Kind.FINALLY_BLOCK);
        f.addChild(visit(ctx.block()));
        return f;
    }

    /* ========= 表达式 ========= */

    @Override
    public AstNode visitPrimaryExpression(JavaParser.PrimaryExpressionContext ctx) {
        return visit(ctx.primary());
    }

    @Override
    public AstNode visitPrimary(JavaParser.PrimaryContext ctx) {
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        if (ctx.THIS() != null || ctx.SUPER() != null) {
            return new AstNode(AstNode.Kind.IDENTIFIER, ctx.getText());
        }
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        if (ctx.identifier() != null) {
            return visit(ctx.identifier());
        }
        if (ctx.typeTypeOrVoid() != null && ctx.CLASS() != null) {
            AstNode type = visit(ctx.typeTypeOrVoid());
            return n(AstNode.Kind.PRIMARY_EXPR, "classLiteral", type);
        }
        return visitChildren(ctx);
    }

    @Override
    public AstNode visitLiteral(JavaParser.LiteralContext ctx) {
        return new AstNode(AstNode.Kind.LITERAL, ctx.getText());
    }

    @Override
    public AstNode visitIdentifier(JavaParser.IdentifierContext ctx) {
        return new AstNode(AstNode.Kind.IDENTIFIER, ctx.getText());
    }

    @Override
    public AstNode visitMethodCallExpression(JavaParser.MethodCallExpressionContext ctx) {
        return visit(ctx.methodCall());
    }

    @Override
    public AstNode visitMethodCall(JavaParser.MethodCallContext ctx) {
        AstNode call = new AstNode(AstNode.Kind.METHOD_CALL_EXPR);
        // 调用目标：标识符 / this / super
        if (ctx.identifier() != null) {
            call.addChild(visit(ctx.identifier()));
        } else if (ctx.THIS() != null) {
            call.addChild(new AstNode(AstNode.Kind.IDENTIFIER, "this"));
        } else if (ctx.SUPER() != null) {
            call.addChild(new AstNode(AstNode.Kind.IDENTIFIER, "super"));
        }
        // 实参
        call.addChild(visit(ctx.arguments()));
        return call;
    }

    @Override
    public AstNode visitArguments(JavaParser.ArgumentsContext ctx) {
        AstNode args = new AstNode(AstNode.Kind.EXPRESSION, "args");
        if (ctx.expressionList() != null) {
            args.addChild(visit(ctx.expressionList()));
        }
        return args;
    }

    @Override
    public AstNode visitExpressionList(JavaParser.ExpressionListContext ctx) {
        AstNode list = new AstNode(AstNode.Kind.EXPRESSION, "list");
        for (JavaParser.ExpressionContext e : ctx.expression()) {
            list.addChild(visit(e));
        }
        return list;
    }

    @Override
    public AstNode visitSquareBracketExpression(JavaParser.SquareBracketExpressionContext ctx) {
        AstNode arrayAccess = new AstNode(AstNode.Kind.ARRAY_ACCESS_EXPR);
        arrayAccess.addChild(visit(ctx.expression(0))); // target
        arrayAccess.addChild(visit(ctx.expression(1))); // index
        return arrayAccess;
    }

    @Override
    public AstNode visitMemberReferenceExpression(JavaParser.MemberReferenceExpressionContext ctx) {
        AstNode member = new AstNode(AstNode.Kind.MEMBER_SELECT_EXPR, ctx.bop.getText());
        member.addChild(visit(ctx.expression()));
        if (ctx.identifier() != null) {
            member.addChild(visit(ctx.identifier()));
        } else if (ctx.methodCall() != null) {
            member.addChild(visit(ctx.methodCall()));
        } else if (ctx.THIS() != null) {
            member.addChild(new AstNode(AstNode.Kind.IDENTIFIER, "this"));
        } else if (ctx.SUPER() != null) {
            member.addChild(new AstNode(AstNode.Kind.IDENTIFIER, "super"));
        } else if (ctx.innerCreator() != null) {
            member.addChild(visit(ctx.innerCreator()));
        } else if (ctx.explicitGenericInvocation() != null) {
            member.addChild(visit(ctx.explicitGenericInvocation()));
        }
        return member;
    }

    @Override
    public AstNode visitBinaryOperatorExpression(JavaParser.BinaryOperatorExpressionContext ctx) {
        String op = ctx.bop.getText();
        AstNode left = visit(ctx.expression(0));
        AstNode right = visit(ctx.expression(1));

        AstNode.Kind kind;
        // 赋值及复合赋值
        if ("=".equals(op) || "+=".equals(op) || "-=".equals(op) || "*=".equals(op)
                || "/=".equals(op) || "&=".equals(op) || "|=".equals(op) || "^=".equals(op)
                || ">>=".equals(op) || ">>>=".equals(op) || "<<=".equals(op) || "%=".equals(op)) {
            kind = AstNode.Kind.ASSIGNMENT_EXPR;
        } else {
            kind = AstNode.Kind.BINARY_EXPR;
        }

        AstNode node = new AstNode(kind, op);
        node.addChild(left);
        node.addChild(right);
        return node;
    }

    @Override
    public AstNode visitUnaryOperatorExpression(JavaParser.UnaryOperatorExpressionContext ctx) {
        String op = ctx.prefix.getText();
        AstNode expr = visit(ctx.expression());
        AstNode node = new AstNode(AstNode.Kind.UNARY_EXPR, op);
        node.addChild(expr);
        return node;
    }

    @Override
    public AstNode visitPostIncrementDecrementOperatorExpression(JavaParser.PostIncrementDecrementOperatorExpressionContext ctx) {
        String op = ctx.postfix.getText();
        AstNode expr = visit(ctx.expression());
        AstNode node = new AstNode(AstNode.Kind.UNARY_EXPR, op);
        node.addChild(expr);
        return node;
    }

    @Override
    public AstNode visitCastExpression(JavaParser.CastExpressionContext ctx) {
        AstNode cast = new AstNode(AstNode.Kind.CAST_EXPR);
        cast.addChild(visit(ctx.typeType(0)));
        cast.addChild(visit(ctx.expression()));
        return cast;
    }

    @Override
    public AstNode visitObjectCreationExpression(JavaParser.ObjectCreationExpressionContext ctx) {
        AstNode obj = new AstNode(AstNode.Kind.OBJECT_CREATION_EXPR, "new");
        obj.addChild(visit(ctx.creator()));
        return obj;
    }

    @Override
    public AstNode visitInstanceOfOperatorExpression(JavaParser.InstanceOfOperatorExpressionContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.INSTANCEOF_EXPR, ctx.bop.getText());
        node.addChild(visit(ctx.expression()));
        if (ctx.typeType() != null) {
            node.addChild(visit(ctx.typeType()));
        } else if (ctx.pattern() != null) {
            node.addChild(visit(ctx.pattern()));
        }
        return node;
    }

    @Override
    public AstNode visitTernaryExpression(JavaParser.TernaryExpressionContext ctx) {
        AstNode cond = visit(ctx.expression(0));
        AstNode thenExpr = visit(ctx.expression(1));
        AstNode elseExpr = visit(ctx.expression(2));
        AstNode node = new AstNode(AstNode.Kind.CONDITIONAL_EXPR, "? :");
        node.addChild(cond);
        node.addChild(thenExpr);
        node.addChild(elseExpr);
        return node;
    }

    @Override
    public AstNode visitExpressionLambda(JavaParser.ExpressionLambdaContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.LAMBDA_EXPR);
        node.addChild(visit(ctx.lambdaExpression()));
        return node;
    }

    /* ========= 默认规则：把未专门处理的规则也包装成 AST ========= */

    @Override
    public AstNode visitChildren(RuleNode node) {
        if (!(node instanceof ParserRuleContext)) {
            return super.visitChildren(node);
        }
        ParserRuleContext ctx = (ParserRuleContext) node;
        String ruleName = JavaParser.ruleNames[ctx.getRuleIndex()];
        AstNode result = new AstNode(AstNode.Kind.UNKNOWN, ruleName);
        int n = ctx.getChildCount();
        for (int i = 0; i < n; i++) {
            ParseTree c = ctx.getChild(i);
            AstNode childAst = c.accept(this);
            if (childAst != null) {
                result.addChild(childAst);
            }
        }
        return result;
    }

    @Override
    public AstNode visitTerminal(TerminalNode node) {
        // 默认不为每个 token 建节点，避免 AST 过于冗长；
        // 关键 token（标识符 / literal）在对应规则里已经处理。
        return null;
    }
}
