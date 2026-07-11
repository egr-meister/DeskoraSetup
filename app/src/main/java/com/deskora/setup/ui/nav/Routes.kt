package com.deskora.setup.ui.nav

/** Central navigation route definitions with safe argument builders. */
object Routes {
    const val ONBOARDING = "onboarding"

    // Bottom navigation destinations
    const val DESK = "desk"
    const val ITEMS = "items"
    const val CHECKLIST = "checklist"
    const val NEEDED = "needed"
    const val SETTINGS = "settings"

    const val EDITOR = "editor"
    const val SEARCH = "search"

    const val SETUP_LIST = "setup_list"

    private const val SETUP_FORM_BASE = "setup_form"
    fun setupForm(setupId: String? = null) =
        if (setupId == null) "$SETUP_FORM_BASE?setupId=" else "$SETUP_FORM_BASE?setupId=$setupId"
    const val SETUP_FORM = "$SETUP_FORM_BASE?setupId={setupId}"

    const val TEMPLATE_GALLERY = "template_gallery"

    private const val TEMPLATE_PREVIEW_BASE = "template_preview"
    fun templatePreview(id: String) = "$TEMPLATE_PREVIEW_BASE/$id"
    const val TEMPLATE_PREVIEW = "$TEMPLATE_PREVIEW_BASE/{templateId}"

    private const val ITEM_FORM_BASE = "item_form"
    fun itemForm(itemId: String? = null) =
        if (itemId == null) "$ITEM_FORM_BASE?itemId=" else "$ITEM_FORM_BASE?itemId=$itemId"
    const val ITEM_FORM = "$ITEM_FORM_BASE?itemId={itemId}"

    private const val ITEM_DETAIL_BASE = "item_detail"
    fun itemDetail(id: String) = "$ITEM_DETAIL_BASE/$id"
    const val ITEM_DETAIL = "$ITEM_DETAIL_BASE/{itemId}"

    const val ZONES = "zones"

    private const val ZONE_FORM_BASE = "zone_form"
    fun zoneForm(zoneId: String? = null) =
        if (zoneId == null) "$ZONE_FORM_BASE?zoneId=" else "$ZONE_FORM_BASE?zoneId=$zoneId"
    const val ZONE_FORM = "$ZONE_FORM_BASE?zoneId={zoneId}"

    const val CABLES = "cables"

    private const val CABLE_FORM_BASE = "cable_form"
    fun cableForm(cableId: String? = null) =
        if (cableId == null) "$CABLE_FORM_BASE?cableId=" else "$CABLE_FORM_BASE?cableId=$cableId"
    const val CABLE_FORM = "$CABLE_FORM_BASE?cableId={cableId}"

    private const val TASK_FORM_BASE = "task_form"
    fun taskForm(taskId: String? = null) =
        if (taskId == null) "$TASK_FORM_BASE?taskId=" else "$TASK_FORM_BASE?taskId=$taskId"
    const val TASK_FORM = "$TASK_FORM_BASE?taskId={taskId}"

    private const val NEEDED_FORM_BASE = "needed_form"
    fun neededForm(neededId: String? = null) =
        if (neededId == null) "$NEEDED_FORM_BASE?neededId=" else "$NEEDED_FORM_BASE?neededId=$neededId"
    const val NEEDED_FORM = "$NEEDED_FORM_BASE?neededId={neededId}"
}
