package com.jsworld.android.autolog.ui.view

object Routes {
    const val SPLASH = "splash"

    const val ADD_CAR = "add_car"

    const val ADD_CAR_FIRST = "add_car?first={first}"
    fun addCarFirst(first: Boolean) = "add_car?first=$first"

    const val CAR_LIST = "car_list"

    const val CAR_DETAIL = "car_detail/{carId}"
    fun carDetail(carId: Long) = "car_detail/$carId"

    const val ADD_MAINTENANCE = "add_maintenance"

    const val ADD_MAINTENANCE_TYPE = "add_maintenance_type/{carId}"
    fun addMaintenanceType(carId: Long) = "add_maintenance_type/$carId"

    const val CAR_MAINTENANCE_ITEM_PICKER = "car_maintenance_item_picker"

    const val EDIT_CAR = "edit_car"

    const val EDIT_MAINTENANCE_SETTING = "edit_maintenance_setting"

    const val ROUTE_HISTORY_LIST = "history_list"
    fun historyListRoute(settingId: Long) = "$ROUTE_HISTORY_LIST/$settingId"

    const val EDIT_MAINTENANCE_HISTORY = "history_edit"

    const val SETTINGS = "settings"
    const val NOTICE = "notice"

    const val EXCEL_EXPORT = "excel_export"

}