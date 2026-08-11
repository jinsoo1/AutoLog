package com.jsworld.android.autolog.presentation.navigation

object Routes {
    const val SPLASH = "splash"

    /** 탭 셸(홈·정비·설정). 앱의 사실상 루트. */
    const val MAIN = "main"

    const val ADD_CAR = "add_car"

    const val ADD_CAR_FIRST = "add_car?first={first}"
    fun addCarFirst(first: Boolean) = "add_car?first=$first"

    /** 차량 관리 — 차량 전환 시트에서 진입 */
    const val CAR_LIST = "car_list"

    /** 정비 항목 관리 — 정비 탭 우상단에서 진입 */
    const val CAR_DETAIL = "car_detail/{carId}"
    fun carDetail(carId: Long) = "car_detail/$carId"

    const val ADD_MAINTENANCE = "add_maintenance"

    /** settingId 를 주면 그 항목이 미리 선택된 상태로 열린다(임박 카드에서 직행). */
    const val ADD_MAINTENANCE_WITH_ARGS = "add_maintenance/{carId}?settingId={settingId}"
    fun addMaintenance(carId: Long, settingId: Long? = null) =
        "add_maintenance/$carId?settingId=${settingId ?: -1L}"

    const val ADD_MAINTENANCE_TYPE = "add_maintenance_type/{carId}"
    fun addMaintenanceType(carId: Long) = "add_maintenance_type/$carId"

    const val CAR_MAINTENANCE_ITEM_PICKER = "car_maintenance_item_picker"

    const val EDIT_CAR = "edit_car"

    /** 정비 항목 상세 — 주기와 교체 내역을 한 화면에서 본다. */
    const val MAINTENANCE_ITEM_DETAIL = "maintenance_item/{settingId}"
    fun maintenanceItemDetail(settingId: Long) = "maintenance_item/$settingId"

    /** 세차·관리 허브 — 정비 탭의 세차 카드에서 진입 */
    const val CARE_DETAIL = "care_detail/{carId}"
    fun careDetail(carId: Long) = "care_detail/$carId"

    /** 항목 주기 수정 — 항목 상세에서 진입 */
    const val EDIT_MAINTENANCE_SETTING = "edit_maintenance_setting"

    const val EDIT_MAINTENANCE_HISTORY = "history_edit"

    /**
     * 주유(충전) 기록 입력·수정. recordId 를 주면 수정 모드.
     * 차량은 현재 차량 컨텍스트를 그대로 쓰므로 인자로 받지 않는다.
     *
     * unit 은 새 기록으로 남길 에너지 종류("L"/"kWh"/"kg").
     * 플러그인 하이브리드는 한 차량에서 주유와 충전을 모두 하므로 인자로 받아야 한다.
     */
    const val FUEL_RECORD = "fuel_record?recordId={recordId}&unit={unit}"
    fun fuelRecord(recordId: Long? = null, unit: String = "L") =
        "fuel_record?recordId=${recordId ?: -1L}&unit=$unit"

    /**
     * 차량 등록 직후 정비 항목 추천. 모든 차량 추가에 붙는다 —
     * 새 차량은 켜진 항목이 0개이기 때문. first 는 끝났을 때 돌아갈 곳을 정한다
     * (첫 차량 = 메인 루트, n번째 = 이전 화면).
     */
    const val MAINTENANCE_STARTER = "maintenance_starter/{carId}?first={first}"
    fun maintenanceStarter(carId: Long, first: Boolean) =
        "maintenance_starter/$carId?first=$first"

    const val SETTINGS = "settings"
    const val NOTICE = "notice"

    const val EXCEL_EXPORT = "excel_export"
}
