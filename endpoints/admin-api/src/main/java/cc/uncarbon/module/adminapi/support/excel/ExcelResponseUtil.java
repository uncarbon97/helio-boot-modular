package cc.uncarbon.module.adminapi.support.excel;

import cc.uncarbon.framework.helium.i18n.context.I18nContext;
import cc.uncarbon.framework.helium.i18n.context.I18nContextHolder;
import cc.uncarbon.framework.helium.i18n.context.timezone.TimezoneInfo;
import cc.uncarbon.module.commons.constant.ExcelConstant;
import cn.hutool.core.date.DatePattern;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Excel Servlet 响应工具
 * <p>统一包办 xlsx 内容类型、带时间戳的下载文件名与 Content-Disposition，Controller 只需一行调用</p>
 * <p>注意：首字节一旦写出，HTTP 状态码便不可再变更；写入方应先完成全部校验（如行数守卫）再开始写流</p>
 */
@UtilityClass
public final class ExcelResponseUtil {

    /**
     * 文件名中的时刻的格式化器；如果是多时区，则按用户时区输出
     */
    public static final DateTimeFormatter FILE_NAME_TIMESTAMP = DatePattern.NORM_DATETIME_FORMATTER.withZone(
            I18nContextHolder.getContextOptional().map(I18nContext::getTimezoneInfo).map(TimezoneInfo::getZoneId)
                    .orElseGet(ZoneId::systemDefault));

    /**
     * Excel 内容写入器
     */
    @FunctionalInterface
    public interface StreamWriter {

        /**
         * 将 xlsx 字节流写入目标输出流
         */
        void write(OutputStream out) throws IOException;
    }


    /**
     * 设置 xlsx 下载响应头，并将输出流交给写入器
     * <p>文件名 = fileNameBase + '-' + 时间戳 + ".xlsx"
     *
     * @param servletResponse HTTP 响应
     * @param fileNameBase    文件名基础部分（不含扩展名）
     * @param writer          xlsx 内容写入器
     */
    public static void writeXlsx(HttpServletResponse servletResponse, String fileNameBase,
                                 StreamWriter writer) throws IOException {
        String fileName = fileNameBase + "-" + FILE_NAME_TIMESTAMP.format(Instant.now()) + ".xlsx";
        servletResponse.setContentType(ExcelConstant.XLSX_CONTENT_TYPE);
        servletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, toContentDisposition(fileName));
        writer.write(servletResponse.getOutputStream());
    }

    /**
     * 生成 attachment 形式的 Content-Disposition
     */
    public static String toContentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment;filename=\"" + encoded + "\"";
    }
}
