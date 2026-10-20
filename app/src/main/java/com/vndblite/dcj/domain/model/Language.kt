package com.vndblite.dcj.domain.model

/**
 * Pure domain enum of VNDB language codes without hardcoded UI paths or colors (A6, A8).
 */
enum class Language(
    val code: String,
    val isCommonOriginLanguage: Boolean = false
) {
    AR("ar", isCommonOriginLanguage = true),
    BG("bg"),
    CA("ca"),
    CS("cs"),
    DE("de", isCommonOriginLanguage = true),
    EL("el"),
    EN("en", isCommonOriginLanguage = true),
    ES("es", isCommonOriginLanguage = true),
    FI("fi"),
    FR("fr", isCommonOriginLanguage = true),
    HR("hr"),
    HU("hu"),
    ID("id", isCommonOriginLanguage = true),
    IT("it"),
    JA("ja", isCommonOriginLanguage = true),
    KO("ko", isCommonOriginLanguage = true),
    MS("ms"),
    NL("nl"),
    NO("no"),
    PL("pl"),
    PT_BR("pt-br", isCommonOriginLanguage = true),
    PT_PT("pt-pt"),
    RO("ro"),
    RU("ru", isCommonOriginLanguage = true),
    SK("sk"),
    SR("sr"),
    SV("sv"),
    TA("ta", isCommonOriginLanguage = true),
    TH("th", isCommonOriginLanguage = true),
    TR("tr", isCommonOriginLanguage = true),
    UK("uk"),
    VI("vi", isCommonOriginLanguage = true),
    ZH_HANS("zh-Hans", isCommonOriginLanguage = true),
    ZH_HANT("zh-Hant", isCommonOriginLanguage = true);

    companion object {
        val originLanguages: List<Language> = entries.filter { it.isCommonOriginLanguage }

        fun fromCode(code: String?): Language? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}
