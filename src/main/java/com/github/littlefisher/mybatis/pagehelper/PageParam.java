package com.github.littlefisher.mybatis.pagehelper;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Description: PageParam.java
 *
 * Created on 2017年12月28日
 *
 * @author jinyanan
 * @version 1.0
 * @since v1.0
 */
@Data
@SuperBuilder
public class PageParam implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 4901705218363381498L;

    /**
     * pageNum
     */
    @Builder.Default
    private int pageNum = 1;

    /**
     * pageSize 每页行数
     */
    @Builder.Default
    private int pageSize = 20;

}