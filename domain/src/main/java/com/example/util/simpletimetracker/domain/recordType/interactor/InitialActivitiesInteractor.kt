package com.example.util.simpletimetracker.domain.recordType.interactor

import com.example.util.simpletimetracker.domain.category.repo.CategoryRepo
import com.example.util.simpletimetracker.domain.category.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import javax.inject.Inject

/**
 * Creates initial activity tiles on first app start.
 * Names are in German because the app targets a single German user.
 */
class InitialActivitiesInteractor @Inject constructor(
    private val recordTypeRepo: RecordTypeRepo,
    private val categoryRepo: CategoryRepo,
    private val recordTypeCategoryRepo: RecordTypeCategoryRepo,
) {

    suspend fun executeIfEmpty() {
        if (recordTypeRepo.getAll().isNotEmpty()) return
        seed()
    }

    private suspend fun seed() {
        data.forEach { entry ->
            val typeId = recordTypeRepo.add(
                RecordType(
                    name = entry.name,
                    icon = entry.icon,
                    color = AppColor(colorId = entry.colorId, colorInt = ""),
                    defaultDuration = 0,
                    note = "",
                ),
            )
            if (entry.category.isNotEmpty()) {
                val categoryId = categoryRepo.add(
                    Category(
                        name = entry.category,
                        color = AppColor(colorId = entry.colorId, colorInt = ""),
                        note = "",
                    ),
                )
                recordTypeCategoryRepo.addCategories(typeId, listOf(categoryId))
            }
        }
    }

    data class Entry(
        val name: String,
        val icon: String,
        val colorId: Int,
        val category: String = "",
    )

    companion object {

        private val data: List<Entry> = listOf(
            Entry(name = "Lernen", icon = "ic_menu_book_24px", colorId = 5),
            Entry(name = "Vorlesung", icon = "ic_school_24px", colorId = 6),
            Entry(name = "Work", icon = "ic_work_24px", colorId = 7),
            Entry(name = "Pause", icon = "ic_free_breakfast_24px", colorId = 12),
            Entry(name = "Schlafen", icon = "ic_single_bed_24px", colorId = 1),
            Entry(name = "Essen", icon = "ic_restaurant_24px", colorId = 9),
            Entry(name = "Cooking", icon = "ic_kitchen_24px", colorId = 10),
            Entry(name = "Chores", icon = "ic_cleaning_services_24px", colorId = 15),
            Entry(name = "Commute", icon = "ic_commute_24px", colorId = 8),
            Entry(name = "Sport", icon = "ic_fitness_center_24px", colorId = 4),
            Entry(name = "Stretch", icon = "ic_self_improvement_24px", colorId = 11),
            Entry(name = "Lesen", icon = "ic_book_24px", colorId = 13),
            Entry(name = "Gitarre", icon = "ic_music_note_24px", colorId = 3),
            Entry(name = "Tinkering", icon = "ic_build_24px", colorId = 16),
            Entry(name = "Social", icon = "ic_people_24px", colorId = 2),
            Entry(name = "Games", icon = "ic_games_24px", colorId = 17),
            Entry(name = "YouTube", icon = "ic_ondemand_video_24px", colorId = 14),
            Entry(name = "Bio", icon = "ic_wc_24px", colorId = 18),
            Entry(name = "Nicht kategorisiert", icon = "ic_help_outline_24px", colorId = 0),
        )
    }
}
