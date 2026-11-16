package com.isep.composeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.isep.composeapp.resources.*
import com.isep.composeapp.ui.AuthViewModel
import com.isep.composeapp.ui.LoginScreen
import com.isep.composeapp.ui.i18n.messageRes
import com.isep.composeapp.ui.navigation.*
import com.isep.composeapp.ui.screens.ListingDetailScreen
import com.isep.composeapp.ui.theme.SwapCampusTheme
import com.isep.composeapp.ui.viewmodel.*
import com.isep.shared.data.AppContainer
import com.isep.shared.domain.entities.Sold
import com.isep.shared.domain.entities.UserRole
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.collectLatest

private data class TopLevelDestination(val route: Any, val label: StringResource, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    SwapCampusTheme {
        val nav = rememberNavController()
        val auth: AuthViewModel = viewModel(factory = viewModelFactory {
            initializer { AuthViewModel(AppContainer.authRepository) }
        })
        val listings: ListingsViewModel = viewModel(factory = viewModelFactory {
            initializer { ListingsViewModel(AppContainer.listingRepository) }
        })
        val user by auth.currentUser.collectAsState()
        val notifications by AppContainer.listingRepository.notifications.collectAsState()
        val all by listings.listings.collectAsState()
        val state by listings.uiState.collectAsState()
        val entry by nav.currentBackStackEntryAsState()
        val destination = entry?.destination
        val tabs = listOf(
            TopLevelDestination(ListingsRoute, Res.string.listings_title, Icons.Default.Home),
            TopLevelDestination(MyListingsRoute, Res.string.my_listings_title, Icons.AutoMirrored.Filled.List),
            TopLevelDestination(ProfileRoute, Res.string.profile_title, Icons.Default.Person)
        )
        val topLevel = tabs.any { destination?.hasRoute(it.route::class) == true }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(listings) {
            listings.messages.collectLatest { message ->
                val resource = when (message) {
                    is UiMessage.Error -> message.error.messageRes()
                    is UiMessage.Success -> when (message.action) {
                        ListingAction.CREATED -> Res.string.message_created
                        ListingAction.UPDATED -> Res.string.message_updated
                        ListingAction.DELETED -> Res.string.message_deleted
                        ListingAction.RESERVED -> Res.string.message_reserved
                        ListingAction.SOLD -> Res.string.message_sold
                        ListingAction.RESTORED -> Res.string.message_restored
                    }
                }
                snackbar.showSnackbar(getString(resource))
            }
        }
        val title = when {
            destination?.hasRoute<MyListingsRoute>() == true -> Res.string.my_listings_title
            destination?.hasRoute<ProfileRoute>() == true -> Res.string.profile_title
            destination?.hasRoute<DetailRoute>() == true -> Res.string.detail_title
            destination?.hasRoute<PostRoute>() == true -> Res.string.post_title
            destination?.hasRoute<EditRoute>() == true -> Res.string.edit_title
            destination?.hasRoute<LoginRoute>() == true -> Res.string.login_title
            else -> Res.string.listings_title
        }
        Scaffold(
            topBar = { TopAppBar(title = { Text(stringResource(title)) }, navigationIcon = {
                if (!topLevel && destination != null) IconButton({ nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back))
                }
            }) },
            bottomBar = { if (topLevel) NavigationBar {
                tabs.forEach { tab -> NavigationBarItem(
                    selected = destination?.hasRoute(tab.route::class) == true,
                    onClick = { nav.navigate(tab.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo<ListingsRoute> { saveState = true }
                    } },
                    icon = { Icon(tab.icon, null) }, label = { Text(stringResource(tab.label)) }
                ) }
            } },
            floatingActionButton = {
                if (destination?.hasRoute<ListingsRoute>() == true || destination?.hasRoute<MyListingsRoute>() == true)
                    FloatingActionButton({ nav.navigate(PostRoute) }) {
                        Icon(Icons.Default.Add, stringResource(Res.string.post_title))
                    }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            NavHost(navController = nav, startDestination = ListingsRoute,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                composable<ListingsRoute> {
                    ListingsScreen(state, listings::filter, { nav.navigate(DetailRoute(it)) }, listings::refresh)
                }
                composable<MyListingsRoute> {
                    if (user?.role == UserRole.STUDENT) {
                        MyListingsScreen(all.filter { it.author.id == user?.id },
                            { nav.navigate(PostRoute) }, { nav.navigate(DetailRoute(it)) })
                    } else LoginPrompt { nav.navigate(LoginRoute) }
                }
                composable<ProfileRoute> {
                    ProfileScreen(user, auth, notifications.filter { it.recipientId == user?.id })
                }
                composable<LoginRoute> {
                    // Only this destination reacts to successful login; popping restores the original listing/form.
                    LaunchedEffect(user?.id) { if (user != null) nav.popBackStack() }
                    LoginScreen(auth)
                }
                composable<DetailRoute> { backStack ->
                    val id = backStack.toRoute<DetailRoute>().id
                    ListingDetailScreen(all.firstOrNull { it.id == id }, user,
                        onEdit = { nav.navigate(EditRoute(id)) },
                        onReserve = { if (user == null) nav.navigate(LoginRoute) else listings.reserve(id) },
                        onSold = { listings.markSold(id) },
                        onRestore = { listings.restore(id) },
                        onDelete = { if (listings.delete(id)) nav.popBackStack() })
                }
                composable<PostRoute> {
                    if (user?.role == UserRole.STUDENT) {
                        PostListingScreen(onSubmit = { titleText, description, price, category, photo ->
                            if (listings.save(null, titleText, description, price, category, photo)) nav.popBackStack()
                        })
                    } else LoginPrompt { nav.navigate(LoginRoute) }
                }
                composable<EditRoute> { backStack ->
                    val id = backStack.toRoute<EditRoute>().id
                    val listing = all.firstOrNull { it.id == id }
                    if (listing == null) Text(stringResource(Res.string.error_listing_not_found))
                    else if (user?.role != UserRole.STUDENT || listing.author.id != user?.id || listing.status is Sold)
                        Text(stringResource(Res.string.error_permission_denied))
                    else PostListingScreen(
                        onSubmit = { titleText, description, price, category, photo ->
                            if (listings.save(id, titleText, description, price, category, photo)) nav.popBackStack()
                        }, initialTitle = listing.title, initialDescription = listing.description,
                        initialPrice = listing.price.toString(), initialCategory = listing.category,
                        initialPhotoUrl = listing.photoUrl.orEmpty(), isEditing = true)
                }
            }
        }
    }
}

@Composable
private fun LoginPrompt(onLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.login_required_hint))
        Button(onLogin) { Text(stringResource(Res.string.action_login)) }
    }
}
