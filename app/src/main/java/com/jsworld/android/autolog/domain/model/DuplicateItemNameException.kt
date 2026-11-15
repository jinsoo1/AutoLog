package com.jsworld.android.autolog.domain.model

/**
 * 같은 이름의 정비 항목이 이미 있을 때.
 *
 * 저장소에는 Context 가 없어 사용자 문구를 만들 수 없다.
 * 화면 쪽(ViewModel)이 이 타입을 보고 현재 언어의 문구로 바꿔 보여준다.
 */
class DuplicateItemNameException(val name: String) :
    IllegalStateException("Maintenance item already exists: $name")
