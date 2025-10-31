package com.example.giftquest.ui.screens

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.giftquest.Routes
import com.example.giftquest.data.model.Item
import com.example.giftquest.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,          // kept for now (Room-era API) – not used here
    onOpenGuessChat: (Long) -> Unit,     // kept for now (Room-era API) – not used here
    onOpenProfile: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))

    val myItems: List<Item> by vm.myItems.collectAsState()
    val herItems: List<Item> by vm.partnerItems.collectAsState()

    val coupleIdProfile by vm.coupleIdProfile.collectAsState()
    val userProfile by vm.userProfile.collectAsState()
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
            // already updated via Firestore; nothing to do here
            saved.remove<String>("editedItem")
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    val authUser = FirebaseAuth.getInstance().currentUser
    val nickname = userProfile?.get("nickname") as? String ?: authUser?.displayName ?: "You"
    val photoUrl = userProfile?.get("photoUrl") as? String ?: ""
    val uid = authUser?.uid ?: "unknown"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    // Profile Section
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    drawerState.close()
                                    onOpenProfile()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                nickname,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(uid.take(10) + "…", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Tap to edit profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                if (pagerState.currentPage == 0) {
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
                        0 -> MyItemsPage(
                            items = myItems,
                            // TODO: once nav uses String IDs, wire these back
                            onEdit = { /* onEditItem(0L) */ },
                            onGuess = { /* onOpenGuessChat(0L) */ }
                        )
                        1 -> {
                            if (coupleIdProfile == null) {
                                HerItemsLinkPage(
                                    onLinkPartner = { code -> vm.linkWithPartner(code) }
                                )
                            } else {
                                PartnerItemsPage(
                                    items = herItems,
                                    onGuess = { /* onOpenGuessChat(0L) */ }
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
private fun MyItemsPage(
    items: List<Item>,
    onEdit: (String) -> Unit,
    onGuess: (String) -> Unit
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
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                    ElevatedCard(
                        // TODO wire when nav uses String IDs: onClick = { onEdit(item.id) }
                        onClick = { /* no-op for now */ },
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap to edit…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerItemsPage(
    items: List<Item>,
    onGuess: (String) -> Unit
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
                        // TODO wire when nav uses String IDs: onClick = { onGuess(item.id) }
                        onClick = { /* no-op for now */ },
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap to guess…", style = MaterialTheme.typography.bodyMedium)
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
