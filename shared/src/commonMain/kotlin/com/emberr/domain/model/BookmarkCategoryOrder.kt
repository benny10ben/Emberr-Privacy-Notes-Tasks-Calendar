package com.emberr.domain.model

import kotlinx.serialization.Serializable

const val BOOKMARK_CATEGORY_ORDER_ENTITY_ID = "bookmark_category_order"

@Serializable
data class BookmarkCategoryOrder(
    val categories: List<String> = emptyList(),
    val updatedAt: Long = 0L
)
