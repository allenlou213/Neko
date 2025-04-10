package org.nekomanga.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.Loading
import org.nekomanga.presentation.components.MangaGrid
import org.nekomanga.presentation.components.MangaList
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.listGridAppBarAction
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.components.showLibraryEntriesAction
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Shapes

@Composable
fun DisplayScreen(
    displayScreenState: State<DisplayScreenState>,
    switchDisplayClick: () -> Unit,
    switchLibraryVisibilityClick: () -> Unit,
    onBackPress: () -> Unit,
    openManga: (Long) -> Unit,
    addNewCategory: (String) -> Unit,
    toggleFavorite: (Long, List<CategoryItem>) -> Unit,
    loadNextPage: () -> Unit,
    retryClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        )
    var longClickedMangaId by remember { mutableStateOf<Long?>(null) }

    /**
     * Close the bottom sheet on back if its open
     */
    BackHandler(enabled = sheetState.isVisible) {
        scope.launch { sheetState.hide() }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(Shapes.sheetRadius),
        sheetContent = {
            Box(modifier = Modifier.defaultMinSize(minHeight = 1.dp)) {
                EditCategorySheet(
                    addingToLibrary = true,
                    categories = displayScreenState.value.categories,
                    cancelClick = { scope.launch { sheetState.hide() } },
                    addNewCategory = addNewCategory,
                    confirmClicked = { selectedCategories ->
                        scope.launch { sheetState.hide() }
                        longClickedMangaId?.let {
                            toggleFavorite(it, selectedCategories)
                        }
                    },
                )
            }
        },
    ) {
        NekoScaffold(
            title = if (displayScreenState.value.titleRes != null) stringResource(id = displayScreenState.value.titleRes!!) else displayScreenState.value.title,
            type = NekoScaffoldType.Title,
            onNavigationIconClicked = onBackPress,
            actions = {
                AppBarActions(
                    actions =
                    listOf(
                        listGridAppBarAction(
                            isList = displayScreenState.value.isList,
                            onClick = switchDisplayClick,
                        ),
                        showLibraryEntriesAction(
                            showEntries = displayScreenState.value.showLibraryEntries,
                            onClick = switchLibraryVisibilityClick,
                        ),
                    ),
                )
            },
        ) { incomingContentPadding ->
            val contentPadding =
                PaddingValues(
                    bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        .asPaddingValues().calculateBottomPadding(),
                    top = incomingContentPadding.calculateTopPadding(),
                )

            val haptic = LocalHapticFeedback.current
            fun mangaLongClick(displayManga: DisplayManga) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!displayManga.inLibrary && displayScreenState.value.promptForCategories) {
                    scope.launch {
                        longClickedMangaId = displayManga.mangaId
                        sheetState.show()
                    }
                } else {
                    toggleFavorite(displayManga.mangaId, emptyList())
                }
            }

            if (displayScreenState.value.isLoading && displayScreenState.value.page == 1) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Loading(
                        Modifier
                            .zIndex(1f)
                            .padding(8.dp)
                            .padding(top = contentPadding.calculateTopPadding())
                            .align(Alignment.TopCenter),
                    )
                }
            } else if (displayScreenState.value.error != null) {
                EmptyScreen(
                    icon = Icons.Default.ErrorOutline,
                    iconSize = 176.dp,
                    message = displayScreenState.value.error,
                    actions = if (displayScreenState.value.page == 1) persistentListOf(Action(R.string.retry, retryClick)) else persistentListOf(),
                    contentPadding = incomingContentPadding,
                )
            } else {
                if (displayScreenState.value.isList) {
                    MangaList(
                        mangaList = displayScreenState.value.filteredDisplayManga,
                        shouldOutlineCover = displayScreenState.value.outlineCovers,
                        contentPadding = contentPadding,
                        onClick = openManga,
                        onLongClick = ::mangaLongClick,
                        lastPage = displayScreenState.value.endReached,
                        loadNextItems = loadNextPage,
                    )
                } else {
                    MangaGrid(
                        mangaList = displayScreenState.value.filteredDisplayManga,
                        shouldOutlineCover = displayScreenState.value.outlineCovers,
                        columns = numberOfColumns(rawValue = displayScreenState.value.rawColumnCount),
                        isComfortable = displayScreenState.value.isComfortableGrid,
                        contentPadding = contentPadding,
                        onClick = openManga,
                        onLongClick = ::mangaLongClick,
                        lastPage = displayScreenState.value.endReached,
                        loadNextItems = loadNextPage,
                    )
                }
                if (displayScreenState.value.isLoading && displayScreenState.value.page != 1) {
                    Box(Modifier.fillMaxSize()) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .padding(bottom = contentPadding.calculateBottomPadding() + 8.dp)
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
