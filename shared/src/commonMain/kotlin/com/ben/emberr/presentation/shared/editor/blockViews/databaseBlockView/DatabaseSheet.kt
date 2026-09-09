package com.ben.emberr.presentation.shared.editor.blockViews.databaseBlockView

/**
 * Every options panel a database block can open. Mobile shows these as stacked bottom sheets and
 * desktop as one anchored dropdown that swaps its body, but both drive off this single list.
 *
 * Each entry's title lives in `sheetTitleFor` and its body in `OptionSheetBody`, both in
 * OptionSheetBody.kt.
 */
enum class DatabaseSheet {
    NONE,

    CELL_OPTIONS,
    COLUMN_OPTIONS,
    RENAME,
    FORMULA,
    CURRENCY_SELECTION,
    AGGREGATION,

    RENAME_VIEW,
    ADD_VIEW,
    TABLE_SETTINGS,
    SORT,
    FILTER,
    GROUP_BY,
    CARD_SIZE,
    SAVE_AS_TEMPLATE,

    TAG_SELECTION,
    FILE_OPTIONS,
    PRIORITY_SELECTION,
    STATUS_SELECTION
}
