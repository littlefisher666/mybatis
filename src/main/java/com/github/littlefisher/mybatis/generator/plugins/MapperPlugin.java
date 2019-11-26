package com.github.littlefisher.mybatis.generator.plugins;

import com.github.littlefisher.mybatis.generator.BlogCommentGenerator;
import com.github.littlefisher.mybatis.generator.plugins.constants.PropertiesConstant;
import com.github.littlefisher.mybatis.generator.plugins.example.AdditionalExampleGenerator;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.config.CommentGeneratorConfiguration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.internal.util.StringUtility;
import tk.mybatis.mapper.MapperException;
import tk.mybatis.mapper.annotation.ColumnType;

/**
 * @author jinyanan
 * @version 1.0
 * @date 2017年3月4日
 * @since v1.0
 */
public class MapperPlugin extends PluginAdapter {

    /**
     * mappers
     */
    private Set<String> mappers = Sets.newHashSet();

    /**
     * caseSensitive
     */
    private boolean caseSensitive = false;

    /**
     * 开始的分隔符，例如mysql为`，sqlserver为[
     */
    private String beginningDelimiter = "";

    /**
     * 结束的分隔符，例如mysql为`，sqlserver为]
     */
    private String endingDelimiter = "";

    /**
     * 数据库模式
     */
    private String schema;

    /**
     * 注释生成器
     */
    private CommentGeneratorConfiguration commentCfg;

    /**
     * author
     */
    private String author;

    /**
     * currentDateStr
     */
    private String currentDateStr;

    /**
     * 是否启用-是
     */
    private static final String TRUE = "TRUE";


    /**
     * 是否对example中的like做补充，对参数左右添加%号
     */
    private boolean modelExampleLikeAdditionEnabled = true;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        // 设置默认的注释生成器
        commentCfg = new CommentGeneratorConfiguration();
        commentCfg.setConfigurationType(BlogCommentGenerator.class.getCanonicalName());
        context.setCommentGeneratorConfiguration(commentCfg);
        context.getProperties().forEach((key, value) -> commentCfg.addProperty((String) key, (String) value));
        // 支持oracle获取注释#114
        if (context.getJdbcConnectionConfiguration() != null) {
            context.getJdbcConnectionConfiguration().addProperty("remarksReporting", TRUE.toLowerCase());
        } else {
            context.getConnectionFactoryConfiguration().addProperty("remarksReporting", TRUE.toLowerCase());
        }
    }

    @Override
    public void setProperties(Properties properties) {
        this.context.getProperties().forEach(properties::put);
        super.setProperties(properties);
        String mappers = this.properties.getProperty(PropertiesConstant.MAPPERS);
        if (StringUtility.stringHasValue(mappers)) {
            this.mappers.addAll(Splitter.on(',').splitToList(mappers));
        } else {
            throw new MapperException("Mapper插件缺少必要的mappers属性!");
        }
        String caseSensitive = this.properties.getProperty(PropertiesConstant.CASE_SENSITIVE);
        if (StringUtility.stringHasValue(caseSensitive)) {
            this.caseSensitive = TRUE.equalsIgnoreCase(caseSensitive);
        }
        String beginningDelimiter = this.properties.getProperty(PropertiesConstant.BEGINNING_DELIMITER);
        if (StringUtility.stringHasValue(beginningDelimiter)) {
            this.beginningDelimiter = beginningDelimiter;
        }
        String endingDelimiter = this.properties.getProperty(PropertiesConstant.ENDING_DELIMITER);
        if (StringUtility.stringHasValue(endingDelimiter)) {
            this.endingDelimiter = endingDelimiter;
        }
        String schema = this.properties.getProperty(PropertiesConstant.SCHEMA);
        if (StringUtility.stringHasValue(schema)) {
            this.schema = schema;
        }
        String authorString = this.properties.getProperty(PropertiesConstant.AUTHOR);
        if (StringUtility.stringHasValue(authorString)) {
            author = authorString;
        }
        String modelExampleLikeAdditionEnabled = this.properties.getProperty(
            PropertiesConstant.MODEL_EXAMPLE_LIKE_ADDITION_ENABLED);
        if (StringUtility.stringHasValue(modelExampleLikeAdditionEnabled)) {
            this.modelExampleLikeAdditionEnabled = TRUE.equals(modelExampleLikeAdditionEnabled);
        }
        currentDateStr = new SimpleDateFormat("yyyy年MM月dd日").format(new Date());
    }

    /**
     * 获取组装后的名称
     *
     * @param name 名称
     * @return 组装后的名称
     */
    private String getDelimiterName(String name) {
        StringBuilder nameBuilder = new StringBuilder();
        if (StringUtility.stringHasValue(schema)) {
            nameBuilder.append(schema);
            nameBuilder.append(".");
        }
        nameBuilder.append(beginningDelimiter);
        nameBuilder.append(name);
        nameBuilder.append(endingDelimiter);
        return nameBuilder.toString();
    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    /**
     * 生成的Mapper接口
     */
    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        // 获取实体类
        FullyQualifiedJavaType entityType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
        // import接口
        mappers.forEach(mapper -> {
            interfaze.addImportedType(new FullyQualifiedJavaType(mapper));
            interfaze.addSuperInterface(new FullyQualifiedJavaType(mapper + "<" + entityType.getShortName() + ">"));
        });
        // import实体类
        interfaze.addImportedType(entityType);

        // 加注释
        interfaze.addJavaDocLine("/**");
        interfaze.addJavaDocLine(" * " + introspectedTable.getFullyQualifiedTable() + " Mapper 接口<br>");
        interfaze.addJavaDocLine(" *");
        interfaze.addJavaDocLine(" * Created on " + currentDateStr);
        interfaze.addJavaDocLine(" * @author " + author);
        interfaze.addJavaDocLine(" * @version 1.0");
        interfaze.addJavaDocLine(" * @since v1.0");
        interfaze.addJavaDocLine(" */");
        return true;
    }

    /**
     * 处理实体类的包和@Table注解
     *
     * @param topLevelClass 类信息
     * @param introspectedTable 表信息
     */
    private void processEntityClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 导入Table注解
        importClassTableAnnotation(topLevelClass, introspectedTable);
        // 导入lombok中@Data注解
        importClassLombokAnnotation(topLevelClass);
        // 引入JPA注解
        importFieldAnnotation(topLevelClass, Column.class);
        // 判断是否有@GeneratedValue注解的字段
        importFieldAnnotation(topLevelClass, GeneratedValue.class, GenerationType.class);
        // 导入主键生成策略
        importFieldAnnotation(topLevelClass, Id.class);
        // 判断field是否是Transient，如果是则导入对应包
        importFieldAnnotation(topLevelClass, Transient.class);
        // 导入列类型
        importFieldAnnotation(topLevelClass, ColumnType.class, JdbcType.class);

        // 构造Builder内部类
        addBuilderInnerClass(topLevelClass, introspectedTable);

        // 实体类注释
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" *");
        topLevelClass.addJavaDocLine(" * " + introspectedTable.getFullyQualifiedTable() + " 实体<br>");
        topLevelClass.addJavaDocLine(" * " + introspectedTable.getRemarks() + "<br>");
        topLevelClass.addJavaDocLine(" *");
        topLevelClass.addJavaDocLine(" * Created on " + currentDateStr);
        topLevelClass.addJavaDocLine(" * @author " + author);
        topLevelClass.addJavaDocLine(" * @version 2.1");
        topLevelClass.addJavaDocLine(" * @since v2.1");
        topLevelClass.addJavaDocLine(" */");
    }

    /**
     * 创建Builder
     *
     * @param topLevelClass 类信息
     * @param introspectedTable 表信息
     */
    private void addBuilderInnerClass(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addImportedType("lombok.experimental.SuperBuilder");
        topLevelClass.addAnnotation("@SuperBuilder");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");
        topLevelClass.addAnnotation("@NoArgsConstructor");
    }

    /**
     * 导入Table注解
     *
     * @param topLevelClass topLevelClass
     * @param introspectedTable introspectedTable
     */
    private void importClassTableAnnotation(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 引入JPA注解
        topLevelClass.addImportedType("javax.persistence.Table");
        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        // 如果包含空格，或者需要分隔符，需要完善
        if (StringUtility.stringContainsSpace(tableName)) {
            tableName = context.getBeginningDelimiter() + tableName + context.getEndingDelimiter();
        }
        // 是否忽略大小写，对于区分大小写的数据库，会有用
        if (caseSensitive && !topLevelClass.getType().getShortName().equals(tableName)) {
            topLevelClass.addAnnotation("@Table(name = \"" + getDelimiterName(tableName) + "\")");
        } else if (!topLevelClass.getType().getShortName().equalsIgnoreCase(tableName)) {
            topLevelClass.addAnnotation("@Table(name = \"" + getDelimiterName(tableName) + "\")");
        } else if (StringUtility.stringHasValue(schema) || StringUtility.stringHasValue(beginningDelimiter)
            || StringUtility.stringHasValue(endingDelimiter)) {
            topLevelClass.addAnnotation("@Table(name = \"" + getDelimiterName(tableName) + "\")");
        }
    }

    /**
     * 导入lombok中的@Data注解
     *
     * @param topLevelClass topLevelClass
     */
    private void importClassLombokAnnotation(TopLevelClass topLevelClass) {
        topLevelClass.addImportedType("lombok.Getter");
        topLevelClass.addImportedType("lombok.Setter");
        topLevelClass.addAnnotation("@Getter");
        topLevelClass.addAnnotation("@Setter");
        topLevelClass.addImportedType("lombok.EqualsAndHashCode");
        topLevelClass.addImportedType("lombok.ToString");
        if (topLevelClass.getSuperClass() != null) {
            topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = true)");
            topLevelClass.addAnnotation("@ToString(callSuper = true)");
        } else {
            topLevelClass.addAnnotation("@EqualsAndHashCode");
            topLevelClass.addAnnotation("@ToString");
        }
    }

    /**
     * field上的注解，需要在class上进行导入
     *
     * @param topLevelClass topLevelClass
     * @param mainAnnotation 字段上的注解
     * @param extAnnotations 该注解所需额外的导入类
     */
    private void importFieldAnnotation(TopLevelClass topLevelClass, Class<?> mainAnnotation,
        Class<?>... extAnnotations) {
        boolean hasAnnotationOnField = Iterators.any(topLevelClass.getFields().iterator(),
            field -> field != null && CollectionUtils.isNotEmpty(field.getAnnotations()) && Iterators
                .any(field.getAnnotations().iterator(), annotation -> StringUtils.isNotBlank(annotation) && annotation
                    .startsWith("@" + mainAnnotation.getSimpleName())));
        if (hasAnnotationOnField) {
            topLevelClass.addImportedType(mainAnnotation.getCanonicalName());
            if (ArrayUtils.isNotEmpty(extAnnotations)) {
                for (Class<?> extAnnotation : extAnnotations) {
                    topLevelClass.addImportedType(extAnnotation.getCanonicalName());
                }
            }
        }
    }

    /**
     * 生成基础实体类
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        processEntityClass(topLevelClass, introspectedTable);
        return true;
    }

    /**
     * 生成实体类注解KEY对象
     */
    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        processEntityClass(topLevelClass, introspectedTable);
        return true;
    }

    /**
     * 生成带BLOB字段的对象 XxxWithBlobs
     */
    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        processEntityClass(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return false;
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return false;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 针对于like，新增左模糊、右模糊、左右模糊的方法
        if (modelExampleLikeAdditionEnabled) {
            topLevelClass.getInnerClasses().stream().filter(
                input -> input.getType().equals(FullyQualifiedJavaType.getGeneratedCriteriaInstance())).findFirst()
                .ifPresent(innerClass -> introspectedTable.getAllColumns().forEach(introspectedColumn -> {
                    if (introspectedColumn.isJdbcCharacterColumn()) {
                        AdditionalExampleGenerator generator = new AdditionalExampleGenerator();
                        generator.getSetLikeMethod(introspectedColumn).forEach(innerClass::addMethod);
                        generator.getSetNotLikeMethod(introspectedColumn).forEach(innerClass::addMethod);
                    }
                }));
        }
        return true;
    }


    /*下面所有return false的方法都不生成。这些都是基础的CRUD方法，使用通用Mapper实现*/

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectAllMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectAllMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectAllElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerApplyWhereMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    // 以下方法，关闭所有跟Example相关的count、update、delete、select方法，这些方法在通用Mapper中已经存在

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapCountByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleSelectiveElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleWithBLOBsElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByExampleWithoutBLOBsElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerCountByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerDeleteByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerSelectByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerSelectByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
        IntrospectedTable introspectedTable) {
        return false;
    }

}