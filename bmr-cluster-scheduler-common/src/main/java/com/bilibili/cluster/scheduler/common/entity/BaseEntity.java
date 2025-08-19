package com.bilibili.cluster.scheduler.common.entity;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.ToString;

/**
 * BaseEntity
 */

@Setter
@Getter
public abstract class BaseEntity {

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ctime;
    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date mtime;

    /**
     * <p>
     * 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。
     */
    private String creator;

    /**
     * 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。
     */
    private String updater;

    /**
     * 是否删除
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted = false;

    protected boolean equals(String child, String other) {
        return objEquals(child, other);
    }

    protected int hashCode(String id) {
        return objHashCode(id);
    }

    protected boolean equals(Long child, Long other) {
        return objEquals(child, other);
    }

    protected int hashCode(Long id) {
        return objHashCode(id);
    }

    protected boolean equals(Integer child, Integer other) {
        return objEquals(child, other);
    }

    protected int hashCode(Integer id) {
        return objHashCode(id);
    }

    protected boolean objEquals(Object child, Object other) {
        if (child == other) {
            return true;
        }
        if (child == null || other == null) {
            return false;
        }
        return child.equals(other);
    }

    protected int objHashCode(Object id) {
        if (id == null) {
            return super.hashCode();
        }
        return id.hashCode();
    }

}
