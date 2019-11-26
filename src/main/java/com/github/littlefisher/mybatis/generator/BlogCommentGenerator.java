package com.github.littlefisher.mybatis.generator;

import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.InnerEnum;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.MergeConstants;
import org.mybatis.generator.internal.util.StringUtility;

/**
 * Created on 2017年1月14日
 *
 * @author jinyanan
 * @version 1.0
 * @since v1.0
 */
public class BlogCommentGenerator implements CommentGenerator {

    /** The properties. */
    private Properties properties;

    /** author */
    private String author = "LittleFisher";

    public BlogCommentGenerator() {
        super();
        properties = new Properties();
    }

    private String getDelimiterName(String name) {
        return name;
    }

    /**
     * 对Mapper.java接口中的各个方法定义注释
     */
    @Override
    public void addGeneralMethodComment(Method method, IntrospectedTable introspectedTable) {
        // 如果带有@Override注解的方法，不进行javadoc
        if (method.getAnnotations()
            .stream()
            .anyMatch(input -> StringUtils.isNotBlank(input) && "@Override".equalsIgnoreCase(input))) {
            return;
        }
        method.addJavaDocLine("/**");
        method.addJavaDocLine(" * Description: " + method.getName() + "<br>");
        method.addJavaDocLine(" *");
        method.addJavaDocLine(" * @author " + author + " <br>");
        List<Parameter> paramList = method.getParameters();
        for (Parameter p : paramList) {
            method.addJavaDocLine(" * @param " + p.getName() + " " + p.getName());
        }
        if (method.getReturnType() != null) {
            method.addJavaDocLine(" * @return " + method.getReturnType()
                .getShortName() + " " + method.getReturnType()
                .getShortName() + "<br>");
        }
        method.addJavaDocLine(" */");

    }

    /**
     * 对实体bean中各个字段(仅数据库中的字段)field增加注释
     */
    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable,
        IntrospectedColumn introspectedColumn) {
        String jdbc = "JDBC";
        String remarks = StringUtility.stringHasValue(introspectedColumn.getRemarks()) ?
            introspectedColumn.getRemarks() : field.getName();
        // 添加@ApiModelProperty注解，用于swaggerUI展示用
        // field.addAnnotation("@ApiModelProperty(\"" + remarks + "\")");
        field.addJavaDocLine("/**");
        String sb = " * " + remarks;
        field.addJavaDocLine(sb);
        field.addJavaDocLine(" */");

        // 对非数据库字段添加@Transient注解
        if (field.isTransient()) {
            field.addAnnotation("@Transient");
        }
        // 对主键字段增加@Id注解
        if (introspectedTable.getPrimaryKeyColumns()
            .stream()
            .anyMatch(column -> introspectedColumn == column)) {
            field.addAnnotation("@Id");
        }
        // 对数据库字段增加@Column注解，该注解用于解决字段名和数据库字段名不同时的映射问题
        String column = introspectedColumn.getActualColumnName();
        if (StringUtility.stringContainsSpace(column) || introspectedTable.getTableConfiguration()
            .isAllColumnDelimitingEnabled()) {
            column = introspectedColumn.getContext()
                .getBeginningDelimiter() + column + introspectedColumn.getContext()
                .getEndingDelimiter();
        }
        if (!column.equals(introspectedColumn.getJavaProperty())) {
            // @Column
            field.addAnnotation("@Column(name = \"" + getDelimiterName(column) + "\")");
        }
        // 自增字段根据不同的数据库，添加不同的@GeneratedValue注解
        if (introspectedColumn.isIdentity()) {
            if (jdbc.equals(introspectedTable.getTableConfiguration()
                .getGeneratedKey()
                .getRuntimeSqlStatement())) {
                field.addAnnotation("@GeneratedValue(generator = \"JDBC\")");
            } else {
                field.addAnnotation("@GeneratedValue(strategy = GenerationType.IDENTITY)");
            }
        } else if (introspectedColumn.isSequenceColumn()) {
            // 在 Oracle 中，如果需要是 SEQ_TABLENAME，那么可以配置为 select SEQ_{1} from dual
            String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
            String sql = MessageFormat.format(introspectedTable.getTableConfiguration()
                .getGeneratedKey()
                .getRuntimeSqlStatement(), tableName, tableName.toUpperCase());
            field.addAnnotation("@GeneratedValue(strategy = GenerationType.IDENTITY, generator = \"" + sql + "\")");
        }
        // 针对于大字段，增加ColumnType注解
        if (introspectedColumn.isBLOBColumn()) {
            JdbcType type = JdbcType.forCode(introspectedColumn.getJdbcType());
            field.addAnnotation("@ColumnType(jdbcType = JdbcType." + type.name() + ")");
        }

    }

    /**
     * 对实体bean中各个字段(仅数据库中没有的字段)field增加注释
     */
    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable) {
        field.addJavaDocLine("/** " + field.getName() + " */");
    }

    /**
     * 获取context下的properties属性
     */
    @Override
    public void addConfigurationProperties(Properties properties) {
        this.properties.putAll(properties);

        String authorString = properties.getProperty("author");
        if (StringUtility.stringHasValue(authorString)) {
            author = authorString;
        }
    }

    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable) {}

    @Override
    public void addClassComment(InnerClass innerClass, IntrospectedTable introspectedTable,
        boolean markAsDoNotDelete) {}

    @Override
    public void addEnumComment(InnerEnum innerEnum, IntrospectedTable introspectedTable) {}

    @Override
    public void addGetterComment(Method method, IntrospectedTable introspectedTable,
        IntrospectedColumn introspectedColumn) {}

    @Override
    public void addSetterComment(Method method, IntrospectedTable introspectedTable,
        IntrospectedColumn introspectedColumn) {}

    /**
     * 对.java文件的头部增加注释，例如copyright等的信息，一般是文件认证信息
     */
    @Override
    public void addJavaFileComment(CompilationUnit compilationUnit) {}

    /**
     * xml中的注释
     */
    @Override
    public void addComment(XmlElement xmlElement) {
        xmlElement.addElement(new TextElement("<!--" + MergeConstants.NEW_ELEMENT_TAG + "-->"));
    }

    @Override
    public void addRootComment(XmlElement rootElement) {}

    @Override
    public void addGeneralMethodAnnotation(Method method, IntrospectedTable introspectedTable,
        Set<FullyQualifiedJavaType> imports) {}

    @Override
    public void addGeneralMethodAnnotation(Method method, IntrospectedTable introspectedTable,
        IntrospectedColumn introspectedColumn, Set<FullyQualifiedJavaType> imports) {}

    @Override
    public void addFieldAnnotation(Field field, IntrospectedTable introspectedTable,
        Set<FullyQualifiedJavaType> imports) {}

    @Override
    public void addFieldAnnotation(Field field, IntrospectedTable introspectedTable,
        IntrospectedColumn introspectedColumn, Set<FullyQualifiedJavaType> imports) {}

    @Override
    public void addClassAnnotation(InnerClass innerClass, IntrospectedTable introspectedTable,
        Set<FullyQualifiedJavaType> imports) {}

    @Override
    public void addModelClassComment(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {}

}