package com.myparser;

public final class AstPrinter {

    private AstPrinter() {
    }

    public static void print(AstNode root) {
        print(root, 0);
    }

    private static void print(AstNode node, int indent) {
        if (node == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append(node.toString());
        System.out.println(sb.toString());

        for (AstNode child : node.getChildren()) {
            print(child, indent + 1);
        }
    }
}
