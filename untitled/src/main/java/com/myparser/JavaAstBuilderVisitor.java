package com.myparser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 ANTLR 生成的 JavaParser + JavaBaseVisitor 构建 Java AST。
 * 已修正 expression() 调用方式，并消除主要 UNKNOWN 节点。
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

    /* ========= 类型声明 ========= */

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
            typeNode = visit(ctx.annotationTypeDeclaration());
        } else if (ctx.recordDeclaration() != null) {
            typeNode = visit(ctx.recordDeclaration());
        } else {
            return visitChildren(ctx);
        }

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
            return new AstNode(AstNode.Kind.ANNOTATION, ctx.annotation().getText());
        }
        return new AstNode(AstNode.Kind.MODIFIER, ctx.getText());
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
        if (ctx.IMPLEMENTS() != null) {
            AstNode impl = n(AstNode.Kind.TYPE, "implements", visit(ctx.typeList(typeListIndex++)));
            clazz.addChild(impl);
        }

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
        AstNode en = new AstNode(AstNode.Kind.ENUM_DECLARATION, ctx.identifier().getText());
        en.addChild(visit(ctx.enumBodyDeclarations()));
        return en;
    }

    @Override
    public AstNode visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        AstNode rec = new AstNode(AstNode.Kind.RECORD_DECLARATION, ctx.identifier().getText());
        if (ctx.recordHeader() != null) rec.addChild(visit(ctx.recordHeader()));
        if (ctx.recordBody() != null) rec.addChild(visit(ctx.recordBody()));
        return rec;
    }

    @Override
    public AstNode visitRecordHeader(JavaParser.RecordHeaderContext ctx) {
        if (ctx.recordComponentList() != null) {
            return visit(ctx.recordComponentList());
        }
        return new AstNode(AstNode.Kind.PARAMETER_LIST, "RecordComponents");
    }

    @Override
    public AstNode visitRecordComponentList(JavaParser.RecordComponentListContext ctx) {
        AstNode list = new AstNode(AstNode.Kind.PARAMETER_LIST, "RecordComponents");
        for (JavaParser.RecordComponentContext rc : ctx.recordComponent()) {
            list.addChild(visit(rc));
        }
        return list;
    }

    @Override
    public AstNode visitRecordComponent(JavaParser.RecordComponentContext ctx) {
        AstNode comp = new AstNode(AstNode.Kind.PARAMETER, ctx.identifier().getText());
        comp.addChild(visit(ctx.typeType()));
        return comp;
    }

    /* ========= Body & Declarations ========= */

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
        if (ctx.SEMI() != null) return null;
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
                if (mod != null && member != null) member.addChildFirst(mod);
            }
            return member;
        }
        return visitChildren(ctx);
    }

    @Override
    public AstNode visitMemberDeclaration(JavaParser.MemberDeclarationContext ctx) {
        if (ctx.methodDeclaration() != null) return visit(ctx.methodDeclaration());
        if (ctx.fieldDeclaration() != null) return visit(ctx.fieldDeclaration());
        if (ctx.constructorDeclaration() != null) return visit(ctx.constructorDeclaration());
        if (ctx.classDeclaration() != null) return visit(ctx.classDeclaration());
        return visitChildren(ctx);
    }

    /* ========= 方法与字段 ========= */

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
        method.addChild(visit(ctx.methodBody()));
        return method;
    }

    @Override
    public AstNode visitMethodBody(JavaParser.MethodBodyContext ctx) {
        if (ctx.block() != null) {
            return visit(ctx.block());
        }
        return new AstNode(AstNode.Kind.EMPTY_STATEMENT, ";");
    }

    @Override
    public AstNode visitTypeTypeOrVoid(JavaParser.TypeTypeOrVoidContext ctx) {
        if (ctx.VOID() != null) {
            return new AstNode(AstNode.Kind.TYPE, "void");
        }
        return visit(ctx.typeType());
    }

    @Override
    public AstNode visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        AstNode ctor = new AstNode(AstNode.Kind.CONSTRUCTOR_DECLARATION, name);
        ctor.addChild(visit(ctx.formalParameters()));
        ctor.addChild(visit(ctx.constructorBody));
        return ctor;
    }

    /* ========= 参数列表 ========= */

    @Override
    public AstNode visitFormalParameters(JavaParser.FormalParametersContext ctx) {
        AstNode params = new AstNode(AstNode.Kind.PARAMETER_LIST);
        if (ctx.receiverParameter() != null) {
            params.addChild(visit(ctx.receiverParameter()));
        }
        if (ctx.formalParameter() != null) {
            params.addChild(visit(ctx.formalParameter()));
        }
        if (ctx.formalParameterList() != null) {
            for (JavaParser.FormalParameterListContext listCtx : ctx.formalParameterList()) {
                for (JavaParser.FormalParameterContext paramCtx : listCtx.formalParameter()) {
                    params.addChild(visit(paramCtx));
                }
            }
        }
        return params;
    }

    @Override
    public AstNode visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        AstNode param = new AstNode(AstNode.Kind.PARAMETER);
        param.addChild(visit(ctx.typeType()));
        param.addChild(visit(ctx.variableDeclaratorId()));
        return param;
    }

    /* ========= 变量定义 ========= */

    @Override
    public AstNode visitVariableDeclarators(JavaParser.VariableDeclaratorsContext ctx) {
        AstNode list = new AstNode(AstNode.Kind.LOCAL_VARIABLE_DECLARATION);
        for (JavaParser.VariableDeclaratorContext v : ctx.variableDeclarator()) {
            list.addChild(visit(v));
        }
        return list;
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
    public AstNode visitVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
        AstNode id = visit(ctx.identifier());
        if (!ctx.LBRACK().isEmpty()) {
            id.setText(id.getText() + "[]".repeat(ctx.LBRACK().size()));
        }
        return id;
    }

    @Override
    public AstNode visitVariableInitializer(JavaParser.VariableInitializerContext ctx) {
        // 修正：这里的 expression() 没有参数，因为规则里只有一个 expression
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return visit(ctx.arrayInitializer());
    }

    @Override
    public AstNode visitArrayInitializer(JavaParser.ArrayInitializerContext ctx) {
        AstNode arrayInit = new AstNode(AstNode.Kind.EXPRESSION, "ArrayInit");
        if (ctx.variableInitializer() != null) {
            for (JavaParser.VariableInitializerContext v : ctx.variableInitializer()) {
                arrayInit.addChild(visit(v));
            }
        }
        return arrayInit;
    }

    @Override
    public AstNode visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        AstNode local = new AstNode(AstNode.Kind.LOCAL_VARIABLE_DECLARATION);
        if (ctx.VAR() != null) {
            local.addChild(new AstNode(AstNode.Kind.TYPE, "var"));
            local.addChild(visit(ctx.identifier()));
            // 修正：这里的 expression() 没有参数
            local.addChild(visit(ctx.expression()));
        } else {
            local.addChild(visit(ctx.typeType()));
            for (JavaParser.VariableDeclaratorContext v : ctx.variableDeclarators().variableDeclarator()) {
                local.addChild(visit(v));
            }
        }
        return local;
    }

    /* ========= 类型系统 ========= */

    @Override
    public AstNode visitTypeType(JavaParser.TypeTypeContext ctx) {
        AstNode typeNode;
        if (ctx.classOrInterfaceType() != null) {
            typeNode = visit(ctx.classOrInterfaceType());
        } else {
            typeNode = visit(ctx.primitiveType());
        }
        if (!ctx.LBRACK().isEmpty()) {
            typeNode.setText(typeNode.getText() + "[]".repeat(ctx.LBRACK().size()));
        }
        return typeNode;
    }

    @Override
    public AstNode visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        return new AstNode(AstNode.Kind.TYPE, ctx.getText());
    }

    @Override
    public AstNode visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
        return new AstNode(AstNode.Kind.TYPE, ctx.getText());
    }

    @Override
    public AstNode visitTypeList(JavaParser.TypeListContext ctx) {
        AstNode list = new AstNode(AstNode.Kind.TYPE, "typeList");
        for (JavaParser.TypeTypeContext t : ctx.typeType()) {
            list.addChild(visit(t));
        }
        return list;
    }

    /* ========= 语句 ========= */

    @Override
    public AstNode visitBlock(JavaParser.BlockContext ctx) {
        AstNode block = new AstNode(AstNode.Kind.BLOCK);
        for (JavaParser.BlockStatementContext b : ctx.blockStatement()) {
            AstNode child = visit(b);
            if (child != null) block.addChild(child);
        }
        return block;
    }

    @Override
    public AstNode visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        if (ctx.localVariableDeclaration() != null) return visit(ctx.localVariableDeclaration());
        if (ctx.statement() != null) return visit(ctx.statement());
        return visitChildren(ctx);
    }

    @Override
    public AstNode visitStatement(JavaParser.StatementContext ctx) {
        // 1. Block (代码块)
        if (ctx.blockLabel != null) return visit(ctx.blockLabel);

        // 2. Assert (断言)
        if (ctx.ASSERT() != null) {
            AstNode node = new AstNode(AstNode.Kind.ASSERT_STATEMENT);
            node.addChild(visit(ctx.expression(0)));
            if (ctx.expression().size() > 1) {
                node.addChild(visit(ctx.expression(1)));
            }
            return node;
        }

        // 3. If (条件)
        if (ctx.IF() != null) {
            return n(AstNode.Kind.IF_STATEMENT, visit(ctx.expression(0)), visit(ctx.statement(0)),
                    ctx.ELSE() != null ? visit(ctx.statement(1)) : null);
        }

        // 4. For (循环)
        if (ctx.FOR() != null) {
            return n(AstNode.Kind.FOR_STATEMENT, visit(ctx.forControl()), visit(ctx.statement(0)));
        }

        // 5. While (循环)
        if (ctx.WHILE() != null) {
            // 区分 while 和 do-while
            if (ctx.DO() != null) {
                return n(AstNode.Kind.DO_WHILE_STATEMENT, visit(ctx.expression(0)), visit(ctx.statement(0)));
            } else {
                return n(AstNode.Kind.WHILE_STATEMENT, visit(ctx.expression(0)), visit(ctx.statement(0)));
            }
        }

        // 6. Try-Catch-Finally (异常处理) [关键修复点]
        if (ctx.TRY() != null) {
            AstNode tryNode = new AstNode(AstNode.Kind.TRY_STATEMENT);
            // try-with-resources
            if (ctx.resourceSpecification() != null) {
                tryNode.addChild(visit(ctx.resourceSpecification()));
            }
            // try body
            if (ctx.block() != null) {
                tryNode.addChild(visit(ctx.block()));
            }
            // catch blocks
            for (JavaParser.CatchClauseContext cc : ctx.catchClause()) {
                tryNode.addChild(visit(cc));
            }
            // finally block
            if (ctx.finallyBlock() != null) {
                tryNode.addChild(visit(ctx.finallyBlock()));
            }
            return tryNode;
        }

        // 7. Switch (分支)
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

        // 8. Synchronized (同步)
        if (ctx.SYNCHRONIZED() != null) {
            AstNode sync = new AstNode(AstNode.Kind.SYNCHRONIZED_STATEMENT);
            sync.addChild(visit(ctx.expression(0)));
            sync.addChild(visit(ctx.block()));
            return sync;
        }

        // 9. Return (返回)
        if (ctx.RETURN() != null) {
            AstNode ret = new AstNode(AstNode.Kind.RETURN_STATEMENT);
            if (ctx.expression() != null && !ctx.expression().isEmpty()) {
                ret.addChild(visit(ctx.expression(0)));
            }
            return ret;
        }

        // 10. Throw (抛出异常)
        if (ctx.THROW() != null) {
            AstNode thr = new AstNode(AstNode.Kind.THROW_STATEMENT);
            thr.addChild(visit(ctx.expression(0)));
            return thr;
        }

        // 11. Break/Continue/Yield
        if (ctx.BREAK() != null) {
            AstNode brk = new AstNode(AstNode.Kind.BREAK_STATEMENT);
            if (ctx.identifier() != null) brk.addChild(visit(ctx.identifier()));
            return brk;
        }
        if (ctx.CONTINUE() != null) {
            AstNode cont = new AstNode(AstNode.Kind.CONTINUE_STATEMENT);
            if (ctx.identifier() != null) cont.addChild(visit(ctx.identifier()));
            return cont;
        }
        if (ctx.YIELD() != null) {
            AstNode yieldNode = new AstNode(AstNode.Kind.YIELD_STATEMENT);
            yieldNode.addChild(visit(ctx.expression(0)));
            return yieldNode;
        }

        // 12. 表达式语句 (赋值、方法调用等)
        if (ctx.statementExpression != null || ctx.switchExpression() != null) {
            AstNode expr = ctx.statementExpression != null
                    ? visit(ctx.statementExpression)
                    : visit(ctx.switchExpression());
            return n(AstNode.Kind.EXPR_STATEMENT, expr);
        }

        // 13. 带标签的语句 (Label: ...)
        if (ctx.identifierLabel != null) {
            AstNode label = new AstNode(AstNode.Kind.LABELED_STATEMENT, ctx.identifierLabel.getText());
            label.addChild(visit(ctx.statement(0)));
            return label;
        }

        // 14. 空语句 (;)
        if (ctx.SEMI() != null) return new AstNode(AstNode.Kind.EMPTY_STATEMENT);

        return visitChildren(ctx);
    }
    @Override
    public AstNode visitCatchClause(JavaParser.CatchClauseContext ctx) {
        AstNode catchNode = new AstNode(AstNode.Kind.CATCH_CLAUSE);
        // 1. 异常类型
        catchNode.addChild(visit(ctx.catchType()));
        // 2. 异常变量名
        catchNode.addChild(visit(ctx.identifier()));
        // 3. 处理代码块
        catchNode.addChild(visit(ctx.block()));
        return catchNode;
    }

    @Override
    public AstNode visitCatchType(JavaParser.CatchTypeContext ctx) {
        // 处理多重捕获 (Type1 | Type2 | ...)
        // 我们可以把它们合并成一个 TYPE 节点，或者创建一个 UNION_TYPE
        // 这里简单起见，返回一个包含所有类型的 TYPE 节点
        AstNode typeNode = new AstNode(AstNode.Kind.TYPE, ctx.getText());
        return typeNode;
    }

    @Override
    public AstNode visitFinallyBlock(JavaParser.FinallyBlockContext ctx) {
        AstNode f = new AstNode(AstNode.Kind.FINALLY_BLOCK);
        f.addChild(visit(ctx.block()));
        return f;
    }
    // --- Try-with-resources 相关 (可选，用于更复杂的 try) ---
    @Override
    public AstNode visitResourceSpecification(JavaParser.ResourceSpecificationContext ctx) {
        return visit(ctx.resources());
    }

    @Override
    public AstNode visitResources(JavaParser.ResourcesContext ctx) {
        AstNode resources = new AstNode(AstNode.Kind.LOCAL_VARIABLE_DECLARATION, "Resources");
        for (JavaParser.ResourceContext rc : ctx.resource()) {
            resources.addChild(visit(rc));
        }
        return resources;
    }

    @Override
    public AstNode visitResource(JavaParser.ResourceContext ctx) {
        AstNode res = new AstNode(AstNode.Kind.VARIABLE_DECLARATOR);
        // 简化处理：直接挂载定义的文本或类型
        if (ctx.classOrInterfaceType() != null) {
            res.addChild(visit(ctx.classOrInterfaceType()));
            res.addChild(visit(ctx.variableDeclaratorId()));
            res.addChild(visit(ctx.expression()));
        } else {
            // 处理 resource 为变量引用的情况
            res.addChild(visit(ctx.qualifiedName()));
        }
        return res;
    }

    @Override
    public AstNode visitForControl(JavaParser.ForControlContext ctx) {
        AstNode control = new AstNode(AstNode.Kind.EXPRESSION, "forControl");
        if (ctx.enhancedForControl() != null) {
            return visit(ctx.enhancedForControl());
        }
        // 标准 for 循环: forInit? ; expression? ; forUpdate?
        if (ctx.forInit() != null) control.addChild(visit(ctx.forInit()));

        // 修正点：expression() 没有参数
        if (ctx.expression() != null) control.addChild(visit(ctx.expression()));

        if (ctx.forUpdate != null) control.addChild(visit(ctx.forUpdate));
        return control;
    }

    @Override
    public AstNode visitForInit(JavaParser.ForInitContext ctx) {
        if (ctx.localVariableDeclaration() != null) return visit(ctx.localVariableDeclaration());
        return visit(ctx.expressionList());
    }

    @Override
    public AstNode visitEnhancedForControl(JavaParser.EnhancedForControlContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.LOCAL_VARIABLE_DECLARATION, "enhancedFor");
        node.addChild(visit(ctx.typeType()));
        node.addChild(visit(ctx.variableDeclaratorId()));
        // 修正：enhancedForControl 里 expression 是唯一的
        node.addChild(visit(ctx.expression()));
        return node;
    }

    @Override
    public AstNode visitSwitchBlockStatementGroup(JavaParser.SwitchBlockStatementGroupContext ctx) {
        AstNode group = new AstNode(AstNode.Kind.BLOCK, "caseGroup");
        for (JavaParser.SwitchLabelContext label : ctx.switchLabel()) {
            group.addChild(visit(label));
        }
        for (JavaParser.BlockStatementContext bs : ctx.blockStatement()) {
            group.addChild(visit(bs));
        }
        return group;
    }

    @Override
    public AstNode visitSwitchLabel(JavaParser.SwitchLabelContext ctx) {
        if (ctx.DEFAULT() != null) return new AstNode(AstNode.Kind.LITERAL, "default");
        if (ctx.constantExpression != null) return visit(ctx.constantExpression);
        if (ctx.enumConstantName != null) return new AstNode(AstNode.Kind.IDENTIFIER, ctx.enumConstantName.getText());
        return new AstNode(AstNode.Kind.UNKNOWN, "case");
    }

    /* ========= 对象创建 ========= */

    @Override
    public AstNode visitObjectCreationExpression(JavaParser.ObjectCreationExpressionContext ctx) {
        AstNode obj = new AstNode(AstNode.Kind.OBJECT_CREATION_EXPR, "new");
        obj.addChild(visit(ctx.creator()));
        return obj;
    }

    @Override
    public AstNode visitCreator(JavaParser.CreatorContext ctx) {
        AstNode creator = new AstNode(AstNode.Kind.TYPE, "creator");
        creator.addChild(visit(ctx.createdName()));
        if (ctx.classCreatorRest() != null) creator.addChild(visit(ctx.classCreatorRest()));
        if (ctx.arrayCreatorRest() != null) creator.addChild(visit(ctx.arrayCreatorRest()));
        return creator;
    }

    @Override
    public AstNode visitCreatedName(JavaParser.CreatedNameContext ctx) {
        return new AstNode(AstNode.Kind.TYPE, ctx.getText());
    }

    @Override
    public AstNode visitClassCreatorRest(JavaParser.ClassCreatorRestContext ctx) {
        return visit(ctx.arguments());
    }

    @Override
    public AstNode visitArrayCreatorRest(JavaParser.ArrayCreatorRestContext ctx) {
        if (ctx.arrayInitializer() != null) return visit(ctx.arrayInitializer());
        return new AstNode(AstNode.Kind.EXPRESSION, "arrayDimension");
    }

    /* ========= 表达式 ========= */

    @Override
    public AstNode visitPrimaryExpression(JavaParser.PrimaryExpressionContext ctx) {
        return visit(ctx.primary());
    }

    @Override
    public AstNode visitPrimary(JavaParser.PrimaryContext ctx) {
        // 修正：primary 里的 expression 是唯一的
        if (ctx.expression() != null) return visit(ctx.expression());
        if (ctx.THIS() != null) return new AstNode(AstNode.Kind.IDENTIFIER, "this");
        if (ctx.literal() != null) return visit(ctx.literal());
        if (ctx.identifier() != null) return visit(ctx.identifier());
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
    public AstNode visitMethodCall(JavaParser.MethodCallContext ctx) {
        AstNode call = new AstNode(AstNode.Kind.METHOD_CALL_EXPR);
        if (ctx.identifier() != null) call.addChild(visit(ctx.identifier()));
        call.addChild(visit(ctx.arguments()));
        return call;
    }

    @Override
    public AstNode visitArguments(JavaParser.ArgumentsContext ctx) {
        AstNode args = new AstNode(AstNode.Kind.EXPRESSION, "args");
        if (ctx.expressionList() != null) args.addChild(visit(ctx.expressionList()));
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
    public AstNode visitMemberReferenceExpression(JavaParser.MemberReferenceExpressionContext ctx) {
        AstNode member = new AstNode(AstNode.Kind.MEMBER_SELECT_EXPR, ".");
        member.addChild(visit(ctx.expression()));
        if (ctx.identifier() != null) member.addChild(visit(ctx.identifier()));
        if (ctx.methodCall() != null) member.addChild(visit(ctx.methodCall()));
        return member;
    }

    @Override
    public AstNode visitBinaryOperatorExpression(JavaParser.BinaryOperatorExpressionContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.BINARY_EXPR, ctx.bop.getText());
        if (ctx.bop.getType() == JavaParser.ASSIGN) node = new AstNode(AstNode.Kind.ASSIGNMENT_EXPR, "=");
        // BinaryOperatorExpressionContext 继承自 ExpressionContext，而 expression 规则里有多个 expression
        // 所以这里必须用 (0) 和 (1)
        node.addChild(visit(ctx.expression(0)));
        node.addChild(visit(ctx.expression(1)));
        return node;
    }

    @Override
    public AstNode visitUnaryOperatorExpression(JavaParser.UnaryOperatorExpressionContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.UNARY_EXPR, ctx.prefix.getText());
        node.addChild(visit(ctx.expression()));
        return node;
    }

    @Override
    public AstNode visitPostIncrementDecrementOperatorExpression(JavaParser.PostIncrementDecrementOperatorExpressionContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.UNARY_EXPR, ctx.postfix.getText());
        node.addChild(visit(ctx.expression()));
        return node;
    }
    

    @Override
    public AstNode visitSquareBracketExpression(JavaParser.SquareBracketExpressionContext ctx) {
        AstNode node = new AstNode(AstNode.Kind.ARRAY_ACCESS_EXPR);
        node.addChild(visit(ctx.expression(0)));
        node.addChild(visit(ctx.expression(1)));
        return node;
    }

    @Override
    public AstNode visitChildren(RuleNode node) {
        if (node instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) node;
            String ruleName = JavaParser.ruleNames[ctx.getRuleIndex()];
            AstNode result = new AstNode(AstNode.Kind.UNKNOWN, ruleName);
            for (int i = 0; i < ctx.getChildCount(); i++) {
                AstNode c = ctx.getChild(i).accept(this);
                if (c != null) result.addChild(c);
            }
            return result;
        }
        return null;
    }
}