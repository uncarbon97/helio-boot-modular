package cc.uncarbon.module.i18n;

import cc.uncarbon.framework.helium.i18n.context.lang.LangInfo;
import cc.uncarbon.framework.helium.i18n.props.HeliumI18nProperties;
import cc.uncarbon.framework.helium.i18n.resolver.lang.HeaderLangResolver;
import cc.uncarbon.framework.helium.i18n.util.LocaleUtil;
import cn.hutool.core.text.CharSequenceUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;

import java.util.Locale;
import java.util.Optional;

/**
 * 国际化语言解析定制
 *
 * <p>本项目语言策略：客户端显式传入 en-US（?lang= 或 X-i18n-Lang 请求头）时才返回英语语义，其余情况固定中文。</p>
 * <p>故覆盖默认的 {@link HeaderLangResolver}，去掉标准 Accept-Language 自动匹配逻辑（浏览器语言不应影响返回语言）。</p>
 *
 * @author Uncarbon
 */
//@Configuration
public class I18nLangResolverConfigurer {

    /**
     * 同名同类型 Bean，使自动装配中 @ConditionalOnMissingBean 的默认实现让位
     */
    @Bean
    public HeaderLangResolver headerLangResolver(HeliumI18nProperties props) {
        return new HeaderLangResolver(props) {

            @Override
            public Optional<LangInfo> resolve(@NonNull HttpServletRequest servletRequest) {

                // 仅识别自定义请求头，如「X-i18n-Lang=en_US」；不回退解析 Accept-Language
                final String headerName = props.getLang().getResolver().getHeaderName();
                String headerVal = CharSequenceUtil.cleanBlank(servletRequest.getHeader(headerName));
                if (CharSequenceUtil.isEmpty(headerVal)) {
                    return Optional.empty();
                }
                Locale headerLocale = LocaleUtil.toLocale(headerVal);
                if (headerLocale == null) {
                    return Optional.empty();
                }
                String languageTag = headerLocale.toLanguageTag();
                if (props.getLang().getSupportedLanguageTags().contains(languageTag)) {
                    return Optional.of(LangInfo.ofSimple(languageTag, headerLocale));
                }
                return Optional.empty();
            }
        };
    }
}
