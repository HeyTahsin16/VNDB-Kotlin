package com.vndblite.dcj.domain.model

/**
 * Pure domain enum of VNDB platform codes (A6, A8).
 */
enum class Platform(
    val code: String,
    val hasDedicatedAsset: Boolean,
    val isMonochromeAsset: Boolean = false
) {
    WIN("win", hasDedicatedAsset = true),
    LIN("lin", hasDedicatedAsset = true),
    MAC("mac", hasDedicatedAsset = true),
    WEB("web", hasDedicatedAsset = true, isMonochromeAsset = true),
    TDO("tdo", hasDedicatedAsset = false),
    IOS("ios", hasDedicatedAsset = true),
    AND("and", hasDedicatedAsset = true),
    BDP("bdp", hasDedicatedAsset = true, isMonochromeAsset = true),
    DOS("dos", hasDedicatedAsset = false),
    DVD("dvd", hasDedicatedAsset = true, isMonochromeAsset = true),
    DRC("drc", hasDedicatedAsset = true),
    NES("nes", hasDedicatedAsset = true),
    SFC("sfc", hasDedicatedAsset = false),
    FM7("fm7", hasDedicatedAsset = true),
    FM8("fm8", hasDedicatedAsset = true),
    FMT("fmt", hasDedicatedAsset = true),
    GBA("gba", hasDedicatedAsset = false),
    GBC("gbc", hasDedicatedAsset = false),
    MSX("msx", hasDedicatedAsset = false),
    NDS("nds", hasDedicatedAsset = true, isMonochromeAsset = true),
    SWI("swi", hasDedicatedAsset = true),
    SW2("sw2", hasDedicatedAsset = false),
    WII("wii", hasDedicatedAsset = true, isMonochromeAsset = true),
    WIU("wiu", hasDedicatedAsset = false),
    N3D("n3d", hasDedicatedAsset = true, isMonochromeAsset = true),
    P88("p88", hasDedicatedAsset = true),
    P98("p98", hasDedicatedAsset = true),
    PCE("pce", hasDedicatedAsset = false),
    PCF("pcf", hasDedicatedAsset = false),
    PSP("psp", hasDedicatedAsset = true, isMonochromeAsset = true),
    PS1("ps1", hasDedicatedAsset = true),
    PS2("ps2", hasDedicatedAsset = true, isMonochromeAsset = true),
    PS3("ps3", hasDedicatedAsset = true, isMonochromeAsset = true),
    PS4("ps4", hasDedicatedAsset = true, isMonochromeAsset = true),
    PS5("ps5", hasDedicatedAsset = true, isMonochromeAsset = true),
    PSV("psv", hasDedicatedAsset = true, isMonochromeAsset = true),
    SMD("smd", hasDedicatedAsset = false),
    SCD("scd", hasDedicatedAsset = false),
    SAT("sat", hasDedicatedAsset = false),
    VND("vnd", hasDedicatedAsset = true),
    X1S("x1s", hasDedicatedAsset = false),
    X68("x68", hasDedicatedAsset = false),
    XB1("xb1", hasDedicatedAsset = true),
    XB3("xb3", hasDedicatedAsset = true),
    XBO("xbo", hasDedicatedAsset = true),
    XXS("xxs", hasDedicatedAsset = true),
    MOB("mob", hasDedicatedAsset = false),
    OTH("oth", hasDedicatedAsset = true);

    companion object {
        fun fromCode(code: String?): Platform? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}
