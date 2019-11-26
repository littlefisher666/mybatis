package com.github.littlefisher.mybatis.generator.plugins.constants;

/**
 * @author jinyanan
 * @since 2019/11/25 17:18
 */
public class PropertiesConstant {

    /**
     * mappers
     */
    public static final String MAPPERS = "mappers";

    /**
     * caseSensitive
     */
    public static final String CASE_SENSITIVE = "caseSensitive";

    /**
     * 开始的分隔符，例如mysql为`，sqlserver为[
     */
    public static final String BEGINNING_DELIMITER = "beginningDelimiter";

    /**
     * 结束的分隔符，例如mysql为`，sqlserver为]
     */
    public static final String ENDING_DELIMITER = "endingDelimiter";

    /**
     * 数据库模式
     */
    public static final String SCHEMA = "schema";

    /**
     * 作者
     */
    public static final String AUTHOR = "author";

    /**
     * 是否启用lombok注解
     */
    public static final String LOMBOK_ENABLED = "lombokEnabled";

    /**
     * 是否对example中的like做补充，对参数左右添加%号
     */
    public static final String MODEL_EXAMPLE_LIKE_ADDITION_ENABLED = "modelExampleLikeAdditionEnabled";
}
