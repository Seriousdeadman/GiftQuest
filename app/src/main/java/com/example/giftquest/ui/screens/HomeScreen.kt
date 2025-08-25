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
    onOpenGuessChat: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))

    val dbItems by vm.items.collectAsState()
    val coupleIdProfile by vm.coupleIdProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by vm.message.collectAsState()

    // Drawer state for the side menu
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // snackbar
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    // handle new item title returned from AddItemScreen
    val saved = navController.currentBackStackEntry?.savedStateHandle
    val returned = saved?.get<String>("newItem")
    LaunchedEffect(returned) {
        if (returned != null) {
            vm.addItem(title = returned)
            saved.remove<String>("newItem")
        }
    }

    // drag state for local list
    var localItems by remember { mutableStateOf(dbItems) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(dbItems, isDragging) { if (!isDragging) localItems = dbItems }

    val tabs = listOf("My Items", "Her Items")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

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

    // profile data (simple: from FirebaseAuth)
    val authUser = FirebaseAuth.getInstance().currentUser
    val nickname = authUser?.displayName ?: "You"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
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

                    // Unlink partner
                    Button(
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                vm.unlinkPartner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = coupleIdProfile != null
                    ) {
                        Text("Un-link partner")
                    }

                    Spacer(Modifier.height(8.dp))

                    // Logout
                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Log out")
                    }
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
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> MyItemsPageReorderable(
                            items = localItems,
                            isDragging = isDragging,
                            reorderState = reorderState,
                            onOpenGuessChat = onOpenGuessChat
                        )
                        1 -> HerItemsPage(
                            coupleIdProfile = coupleIdProfile,
                            onLinkPartner = { code -> vm.linkWithPartner(code) }
                        )
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
                            onClick = { if (!isDragging) onOpenGuessChat(item.id) },
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

@Composable
private fun HerItemsPage(
    coupleIdProfile: String?,
    onLinkPartner: (String) -> Unit
) {
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    var partnerCodeInput by remember { mutableStateOf("") }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (coupleIdProfile == null) {
            Text("Link with your partner to share wishlists", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("Your invite code (tap to copy):")
            Spacer(Modifier.height(8.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(myUid))
                        android.widget.Toast.makeText(ctx, "Copied invite code", android.widget.Toast.LENGTH_SHORT).show()
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
            Text("Have their code? Paste it to link:")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = partnerCodeInput,
                onValueChange = { partnerCodeInput = it },
                placeholder = { Text("Enter partner UID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val code = partnerCodeInput.trim()
                    if (code.isNotEmpty()) onLinkPartner(code)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Link Partner") }
        } else {
            Text("You’re linked ✅", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Couple ID: $coupleIdProfile")
            Spacer(Modifier.height(16.dp))
            Text("Both of you now see & edit the same wishlist.")
        }
    }
}
