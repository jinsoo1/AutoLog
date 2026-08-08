package com.jsworld.android.autolog.domain.model

/**
 * 온보딩에서 고르는 정비 관리 스타일.
 * "유저 등급"이 아니라 관리 스타일로 이름 지었다 — 가볍게 써도 틀린 게 아니라는 뉘앙스.
 */
enum class MaintenanceStarterPack(
    val title: String,
    val description: String
) {
    LIGHT("가볍게", "누구나 챙기는 핵심 소모품만"),
    STANDARD("꼼꼼하게", "자주 손 가는 항목까지 넉넉하게"),
    FULL("빈틈없이", "점검 항목까지 전부 관리해요")
}
