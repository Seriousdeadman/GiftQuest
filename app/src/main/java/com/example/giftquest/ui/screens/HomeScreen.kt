package com.example.giftquest.ui.screens

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.giftquest.Routes
import com.example.giftquest.data.local.ItemEntity
import com.example.giftquest.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onOpenGuessChat: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))

    val myItems: List<ItemEntity> by vm.myItems.collectAsState()
    val herItems: List<ItemEntity> by vm.partnerItems.collectAsState()

    val coupleIdProfile by vm.coupleIdProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by vm.message.collectAsState()

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val saved = navController.currentBackStackEntry?.savedStateHandle
    val returned = saved?.get<String>("newItem")
    val editedItem = saved?.get<String>("editedItem")
    LaunchedEffect(returned) {
        if (returned != null) {
            vm.addItem(title = returned)
            saved.remove<String>("newItem")
        }
    }
    LaunchedEffect(editedItem) {
        if (editedItem != null) {
            // Item was edited, no need to do anything special since it's already updated
            saved.remove<String>("editedItem")
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Reorder only MY list
    var localItems by remember { mutableStateOf(myItems) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(myItems, isDragging) { if (!isDragging) localItems = myItems }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (!isDragging) isDragging = true
            localItems = localItems.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            vm.reorder(localItems.map { it.id })
            isDragging = false
        }
    )

    val authUser = FirebaseAuth.getInstance().currentUser
    val nickname = authUser?.displayName ?: "You"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val uid = authUser?.uid ?: "unknown"
                            Text(uid.take(10) + "…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                vm.unlinkPartner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = coupleIdProfile != null
                    ) { Text("Un-link partner") }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Log out") }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GiftQuest") },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Account menu")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (pagerState.currentPage == 0 && !isDragging) {
                    FloatingActionButton(onClick = onAddItem) { Text("+") }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("My Items") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Her Items") }
                    )
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> MyItemsPageReorderable(
                            items = localItems,
                            isDragging = isDragging,
                            reorderState = reorderState,
                            onEditItem = onEditItem,
                            onOpenGuessChat = onOpenGuessChat // TODO: later change to edit/details screen if you want
                        )
                        1 -> {
                            if (coupleIdProfile == null) {
                                HerItemsLinkPage(
                                    onLinkPartner = { code -> vm.linkWithPartner(code) }
                                )
                            } else {
                                // SAME card UI as "My Items", but no drag handle, and tap opens AI chat
                                PartnerItemsPage(
                                    items = herItems,
                                    onOpenGuessChat = onOpenGuessChat
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyItemsPageReorderable(
    items: List<ItemEntity>,
    isDragging: Boolean,
    reorderState: ReorderableLazyListState,
    onEditItem: (Long) -> Unit,
    onOpenGuessChat: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (items.isEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Start your first wishlist ✨", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap the + button to add your first mystery item.")
                }
            }
        } else {
            Text("My Items", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().reorderable(reorderState),
                state = reorderState.listState
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                    ReorderableItem(reorderState, key = item.id) { _ ->
                        ElevatedCard(
                            onClick = { if (!isDragging) onEditItem(item.id) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tap to edit…", style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(
                                    imageVector = Icons.Filled.DragHandle,
                                    contentDescription = "Drag",
                                    modifier = Modifier.detectReorder(reorderState)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * SAME visual layout as MyItems cards, but:
 * - no drag behaviors/handle
 * - tap opens guess chat for the partner's item
 */
@Composable
private fun PartnerItemsPage(
    items: List<ItemEntity>,
    onOpenGuessChat: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (items.isEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("No mystery items yet ✨", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Your partner hasn't added any items to their wishlist yet.")
                }
            }
        } else {
            Text("Her Items", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                    ElevatedCard(
                        onClick = { onOpenGuessChat(item.id) }, // open guess chat
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("Tap to guess…", style = MaterialTheme.typography.bodyMedium)
                            }
                            // No drag handle here - same visual layout but no drag functionality
                        }
                    }
                }
            }
        }
    }
}

/** Link UI when not yet paired */
@Composable
private fun HerItemsLinkPage(
    onLinkPartner: (String) -> Unit
) {
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    var partnerCodeInput by remember { mutableStateOf("") }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Link with your partner to share wishlists", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("Share your invite code with your partner:")
        Spacer(Modifier.height(8.dp))
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(myUid))
                    android.widget.Toast
                        .makeText(ctx, "Copied invite code", android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(myUid, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text("Tap to copy", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Or enter your partner's code to link:")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = partnerCodeInput,
            onValueChange = { partnerCodeInput = it },
            placeholder = { Text("Enter partner's invite code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val code = partnerCodeInput.trim()
                if (code.isNotEmpty()) onLinkPartner(code)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = partnerCodeInput.trim().isNotEmpty()
        ) { Text("Link Partner") }
        
        Spacer(Modifier.height(16.dp))
        Text(
            "Once linked, you'll be able to see each other's mystery items and guess what they want!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
