package com.isep.composeapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.isep.composeapp.resources.*
import com.isep.composeapp.ui.i18n.text
import com.isep.shared.domain.entities.Category
import org.jetbrains.compose.resources.stringResource

@Composable
fun CategoryChips(selected: Category?, includeAll: Boolean = false, onSelect: (Category?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).testTag("category-chips"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeAll) FilterChip(selected == null, { onSelect(null) },
            label = { Text(stringResource(Res.string.category_all)) })
        Category.entries.forEach { category ->
            FilterChip(selected == category, { onSelect(category) }, label = { Text(category.text()) })
        }
    }
}
