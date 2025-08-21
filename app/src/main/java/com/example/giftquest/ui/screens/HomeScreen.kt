package com.example.giftquest.ui.screens

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.giftquest.Routes
import com.example.giftquest.ui.components.AppTopBar
import com.example.giftquest.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import org.burnoutcrew.reorderable.*
import com.example.giftquest.data.local.ItemEntity
import androidx.compose.material3.TopAppBar
import com.google.firebase.auth.FirebaseAuth



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onAddItem: () -> Unit,
    onOpenGuessChat: (Long) -> Unit
) {
    // ViewModel (DB-backed)
    val app = LocalContext.current.applicationContext as Application
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val dbItems: List<ItemEntity> by vm.items.collectAsState()

    // Receive AddItem result → persist via VM
    val saved = navController.currentBackStackEntry?.savedStateHandle
    val returned = saved?.get<String>("newItem")
    LaunchedEffect(returned) {
        if (returned != null) {
            vm.addItem(title = returned)   // <- write to Firestore; listener mirrors to Room
            saved.remove<String>("newItem")
        }
    }



    // Local working list for drag UI. Only sync when NOT dragging.
    var localItems by remember { mutableStateOf(dbItems) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync from DB to local only when not dragging
    LaunchedEffect(dbItems, isDragging) {
        if (!isDragging) localItems = dbItems
    }

    var myShareCode by remember { mutableStateOf(UUID.randomUUID().toString().take(8)) }
    var partnerCodeInput by remember { mutableStateOf("") }

    val tabs = listOf("My Items", "Her Items")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Reorderable list state
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (!isDragging) isDragging = true // first move → start dragging
            localItems = localItems.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            vm.reorder(localItems.map { it.id })
            isDragging = false
        }
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GiftQuest") },
                actions = {
                    TextButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) { Text("Log out") }
                }
            )
        },
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
                        myShareCode = myShareCode,
                        partnerCodeInput = partnerCodeInput,
                        onPartnerCodeChange = { partnerCodeInput = it },
                        onLinkPartner = { partnerCodeInput = "" }
                    )
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
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState),
                state = reorderState.listState
            ) {
                itemsIndexed(
                    items,
                    key = { _, item -> item.id } // stable key!
                ) { _, item ->
                    ReorderableItem(reorderState, key = item.id) { _ ->
                        ElevatedCard(
                            // IMPORTANT: clickable only when NOT dragging
                            onClick = { if (!isDragging) onOpenGuessChat(item.id) },
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tap to guess…", style = MaterialTheme.typography.bodyMedium)
                                }
                                // DRAG HANDLE ONLY — avoids click/drag conflict
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
    myShareCode: String,
    partnerCodeInput: String,
    onPartnerCodeChange: (String) -> Unit,
    onLinkPartner: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Her Items", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Share your code so your partner can link with you. Once linked, you’ll see her mystery items here.")
        Spacer(Modifier.height(12.dp))
        Text("Your link code:")
        Spacer(Modifier.height(8.dp))
        ElevatedCard { Text(myShareCode, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Have her code? Paste it to link:")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = partnerCodeInput,
            onValueChange = onPartnerCodeChange,
            placeholder = { Text("Enter partner code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLinkPartner, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Link Partner")
        }
    }
}
