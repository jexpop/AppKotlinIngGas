package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.CategoryGroup
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import com.jexpop.appkotlininggas.data.model.CategoryGroupInsert

class CategoryRepository {

    suspend fun getAllGroups(): Result<List<CategoryGroup>> {
        return runCatching {
            supabase.from("category_group")
                .select {
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<CategoryGroup>()
        }
    }

    suspend fun insertGroup(group: CategoryGroup): Result<Unit> {
        return runCatching {
            supabase.from("category_group").insert(
                CategoryGroupInsert(
                    parentId = group.parentId,
                    description = group.description,
                    periodicity = group.periodicity,
                    sortOrder = group.sortOrder,
                    flowType = group.flowType
                )
            )
        }
    }

    suspend fun updateGroup(group: CategoryGroup): Result<Unit> {
        return runCatching {
            supabase.from("category_group").update({
                set("description", group.description)
                set("periodicity", group.periodicity)
                set("sort_order", group.sortOrder)
                set("flow_type", group.flowType)
                if (group.parentId != null) {
                    set("parent_id", group.parentId)
                } else {
                    set("parent_id", null as Int?)
                }
            }) {
                filter { eq("id", group.id!!) }
            }
        }
    }

    suspend fun deleteGroup(id: Int): Result<Unit> {
        return runCatching {
            supabase.from("category_group").delete {
                filter { eq("id", id) }
            }
        }
    }

    suspend fun hasTransactions(groupId: Int): Result<Boolean> {
        return runCatching {
            val result = supabase.from("transaction")
                .select {
                    filter { eq("group_id", groupId) }
                }
                .decodeList<Map<String, String>>()
            result.isNotEmpty()
        }
    }

    suspend fun recalculateSortOrder(
        groups: List<CategoryGroup>,
        parentId: Int? = null,
        parentSortOrder: String = ""
    ): Result<Unit> {
        return runCatching {
            val children = groups
                .filter { it.parentId == parentId }
                .sortedBy { it.sortOrder }

            children.forEachIndexed { index, group ->
                val newSortOrder = parentSortOrder + (index + 1).toString().padStart(3, '0')
                supabase.from("category_group").update({
                    set("sort_order", newSortOrder)
                }) {
                    filter { eq("id", group.id!!) }
                }
                // Recalcular hijos recursivamente
                recalculateSortOrder(groups, group.id, newSortOrder).getOrThrow()
            }
        }
    }

}