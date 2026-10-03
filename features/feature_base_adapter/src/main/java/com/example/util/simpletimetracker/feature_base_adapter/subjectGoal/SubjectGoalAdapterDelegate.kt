package com.example.util.simpletimetracker.feature_base_adapter.subjectGoal

import androidx.core.view.isVisible
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemSubjectGoalLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.subjectGoal.SubjectGoalViewData as ViewData

fun createSubjectGoalAdapterDelegate(
    onItemClick: (ViewData) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        root.setOnClickListener { onItemClick(item) }

        itemSubjectGoalName.text = item.name

        itemSubjectGoalBar.progress = item.percent
        itemSubjectGoalBar.progressTintList =
            android.content.res.ColorStateList.valueOf(item.color)
        itemSubjectGoalBar.isVisible = item.hasTarget
        itemSubjectGoalProgress.text = item.progressText
        itemSubjectGoalProgress.isVisible = item.hasTarget
        itemSubjectGoalEcts.text = item.ectsText
        itemSubjectGoalEcts.isVisible = item.ectsText.isNotEmpty()
        itemSubjectGoalNoTarget.isVisible = !item.hasTarget
    }
}
