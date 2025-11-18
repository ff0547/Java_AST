package com.myparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyTestFile {

    // 字段：基本类型、引用类型、泛型
    private int counter = 0;
    private String name;
    private List<String> names = new ArrayList<>();

    // 构造函数
    public MyTestFile(String name) {
        this.name = name;
        this.names.add(name);
    }

    // if / else if / else + 赋值和比较运算
    public int adjust(int value) {
        if (value > 10) {
            value = value - 2;
        } else if (value < 0) {
            value = 0;
        } else {
            value = value + 1;
        }
        return value;
    }

    // while / do-while / for / for-each
    public void loopDemo() {
        int i = 0;

        // while 循环
        while (i < 2) {
            counter = counter + i;
            i++;
        }

        // do-while 循环
        int j = 0;
        do {
            counter = counter + j;
            j++;
        } while (j < 2);

        // 传统 for 循环
        for (int k = 0; k < 3; k++) {
            counter = counter + k;
        }

        // 增强 for 循环
        for (String s : names) {
            System.out.println("name = " + s);
        }
    }

    // switch 语句
    public String describe(int code) {
        String result;
        switch (code) {
            case 0:
                result = "ZERO";
                break;
            case 1:
                result = "ONE";
                break;
            case 2:
            case 3:
                result = "TWO_OR_THREE";
                break;
            default:
                result = "OTHER";
                break;
        }
        return result;
    }

    // try-catch-finally
    public void exceptionDemo() {
        try {
            int x = 1 / 0;
            System.out.println(x); // 实际上不会执行到
        } catch (ArithmeticException e) {
            System.out.println("catch: " + e.getMessage());
        } finally {
            System.out.println("finally block");
        }
    }

    // 静态泛型方法
    public static <T> List<T> copy(List<T> src) {
        List<T> dest = new ArrayList<>();
        for (T t : src) {
            dest.add(t);
        }
        return dest;
    }

    // 静态方法 + 二元运算
    public static int twice(int x) {
        return x * 2;
    }

    // 静态内部类
    public static class Inner {
        private int value;

        public Inner(int value) {
            this.value = value;
        }

        public int inc() {
            return value + 1;
        }
    }

    // main：对象创建、方法调用、lambda 表达式等
    public static void main(String[] args) {
        MyTestFile test = new MyTestFile("demo");

        int base = test.adjust(5);
        int result = twice(base);

        test.loopDemo();
        test.exceptionDemo();

        List<Integer> nums = Arrays.asList(1, 2, 3);
        // lambda 表达式 + 方法调用
        nums.forEach(x -> System.out.println("num = " + x));

        Inner in = new Inner(result);
        int inc = in.inc();

        System.out.println("base = " + base);
        System.out.println("result = " + result);
        System.out.println("inc = " + inc);
        System.out.println("describe(2) = " + test.describe(2));
    }
}
