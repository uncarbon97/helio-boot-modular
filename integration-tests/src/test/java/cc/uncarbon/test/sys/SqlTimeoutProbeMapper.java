package cc.uncarbon.test.sys;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * SQL 超时探针 Mapper（仅集成测试上下文装配）
 * <p>
 * 语句级超时 1 秒 + SQL 固定睡眠 3 秒，稳定触发 JDBC 查询超时；
 * 依赖 MySQL SLEEP() 函数，与 IT profile 数据源一致
 */
public interface SqlTimeoutProbeMapper {

    /**
     * @return SLEEP 返回值 0（正常情况不会返回，超时先到）
     */
    @Select("SELECT SLEEP(3)")
    @Options(timeout = 1)
    Integer sleepThreeSeconds();
}
