package com.github.littlefisher.mybatis.generator.plugins.example;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import com.google.common.collect.Lists;
import java.util.List;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.codegen.ibatis2.Ibatis2FormattingUtilities;

/**
 * @author jinyanan
 * @since 2019/11/25 17:20
 */
public class AdditionalExampleGenerator {

    public List<Method> getSetLikeMethod(IntrospectedColumn introspectedColumn) {
        List<Method> methodList = Lists.newArrayList();
        methodList.add(getSetLikeWithLeftFuzzinessMethod(introspectedColumn));
        methodList.add(getSetLikeWithRightFuzzinessMethod(introspectedColumn));
        methodList.add(getSetLikeWithAllFuzzinessMethod(introspectedColumn));
        return methodList;
    }

    public List<Method> getSetNotLikeMethod(IntrospectedColumn introspectedColumn) {
        List<Method> methodList = Lists.newArrayList();
        methodList.add(getSetNotLikeWithLeftFuzzinessMethod(introspectedColumn));
        methodList.add(getSetNotLikeWithRightFuzzinessMethod(introspectedColumn));
        methodList.add(getSetNotLikeWithAllFuzzinessMethod(introspectedColumn));
        return methodList;
    }

    private Method getSetLikeWithLeftFuzzinessMethod(IntrospectedColumn introspectedColumn) {
        return getSingleValueMethod(introspectedColumn, "LeftLike", "like", FuzzinessPosition.LEFT);
    }

    private Method getSetLikeWithRightFuzzinessMethod(IntrospectedColumn introspectedColumn) {
        return getSingleValueMethod(introspectedColumn, "RightLike", "like", FuzzinessPosition.RIGHT);
    }

    private Method getSetLikeWithAllFuzzinessMethod(IntrospectedColumn introspectedColumn) {
        return getSingleValueMethod(introspectedColumn, "LeftRightLike", "like", FuzzinessPosition.ALL);
    }

    private Method getSetNotLikeWithLeftFuzzinessMethod(IntrospectedColumn introspectedColumn) {
        return getSingleValueMethod(introspectedColumn, "NotLeftLike", "not like", FuzzinessPosition.LEFT);
    }

    private Method getSetNotLikeWithRightFuzzinessMethod(IntrospectedColumn introspectedColumn) {
        return getSingleValueMethod(introspectedColumn, "NotRightLike", "not like", FuzzinessPosition.RIGHT);
    }

    private Method getSetNotLikeWithAllFuzzinessMethod(IntrospectedColumn introspectedColumn) {
        return getSingleValueMethod(introspectedColumn, "NotLeftRightLike", "not like", FuzzinessPosition.ALL);
    }

    private Method getSingleValueMethod(IntrospectedColumn introspectedColumn, String nameFragment, String operator,
        FuzzinessPosition position) {
        Method method = new Method();
        method.setVisibility(JavaVisibility.PUBLIC);
        method.addParameter(new Parameter(introspectedColumn.getFullyQualifiedJavaType(), "value"));
        StringBuilder sb = new StringBuilder();
        sb.append(introspectedColumn.getJavaProperty());
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        sb.insert(0, "and");
        sb.append(nameFragment);
        method.setName(sb.toString());
        method.setReturnType(FullyQualifiedJavaType.getCriteriaInstance());
        sb.setLength(0);

        if (stringHasValue(introspectedColumn.getTypeHandler())) {
            sb.append("add");
            sb.append(introspectedColumn.getJavaProperty());
            sb.setCharAt(3, Character.toUpperCase(sb.charAt(3)));
            sb.append("Criterion(\"");
        } else {
            sb.append("addCriterion(\"");
        }

        sb.append(Ibatis2FormattingUtilities.getAliasedActualColumnName(introspectedColumn));
        sb.append(' ');
        sb.append(operator);
        sb.append("\", ");
        if (position == FuzzinessPosition.LEFT) {
            sb.append("\"%\" + ");
            sb.append("value");
        } else if (position == FuzzinessPosition.RIGHT) {
            sb.append("value");
            sb.append("+ \"%\"");
        } else if (position == FuzzinessPosition.ALL) {
            sb.append("\"%\" + ");
            sb.append("value");
            sb.append("+ \"%\"");
        } else {
            throw new IllegalArgumentException("模糊的百分号放置位置枚举，不可为空");
        }

        sb.append(", \"");
        sb.append(introspectedColumn.getJavaProperty());
        sb.append("\");");
        method.addBodyLine(sb.toString());
        method.addBodyLine("return (Criteria) this;");

        return method;
    }

    private enum FuzzinessPosition {
        /**
         * 左模糊
         */
        LEFT,
        /**
         * 右模糊
         */
        RIGHT,
        /**
         * 左右模糊
         */
        ALL,
        ;
    }
}
