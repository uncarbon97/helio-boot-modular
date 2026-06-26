package cc.uncarbon.module.adminapi.annotation;

import java.lang.annotation.*;


/**
 * 放在 Controller 方法上，记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SysOperateLog {

    /**
     * 操作内容，如「新增部门」
     */
    String operation();

    /**
     * 主模块
     */
    String mainModule() default "";

    /**
     * 副模块
     */
    String subModule() default "";

    /**
     * 业务号
     */
    String bizNo() default "";

    /**
     * 额外业务信息
     */
    String bizExtra() default "";

    /**
     * 是否同步保存至系统操作日志数据表中
     * true = 同步保存：如果开启了事务/事务注解，若系统操作日志保存失败，则会抛出异常触发回滚，使得本次操作也失败
     * false = 异步保存：若系统操作日志保存失败，不影响本次操作
     */
    boolean syncSave() default false;

    /**
     * 保存系统操作日志至数据表的时机
     * 默认为仅「成功时」
     * 多个输入值间默认为「或」关系
     */
    When[] when() default When.SUCCESS;
    enum When {
        /**
         * 成功时
         */
        SUCCESS,

        /**
         * 失败时
         */
        FAILED,
    }

}
