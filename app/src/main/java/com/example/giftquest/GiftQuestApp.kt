package com.example.giftquest

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.giftquest.ui.screens.*
import com.example.giftquest.ui.theme.GiftQuestTheme
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val ADD_ITEM = "add_item"
    const val EDIT_ITEM = "edit_item"
    const val EDIT_ITEM_ROUTE = "edit_item/{itemId}"
    const val PROFILE = "profile"
    const val GUESS_CHAT = "guess_chat"
    const val GUESS_CHAT_ROUTE = "guess_chat/{itemId}"
    const val HER_ITEMS = "herItems"
}

@Composable
fun GiftQuestApp() {
    GiftQuestTheme {
        val nav = rememberNavController()
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        val start = if (isLoggedIn) Routes.HOME else Routes.LOGIN

        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = nav, startDestination = start) {

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            nav.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onCreateAccount = { nav.navigate(Routes.SIGN_UP) },
                        onForgotPassword = { /* TODO: nav to reset screen later */ }
                    )
                }

                composable(Routes.SIGN_UP) {
                    SignUpScreen(
                        onBack = { nav.popBackStack() },
                        onSignedUp = {
                            nav.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        navController = nav,
                        onAddItem = { nav.navigate(Routes.ADD_ITEM) },
                        onEditItem = { itemId -> nav.navigate("edit_item/$itemId") },
                        onOpenGuessChat = { itemId -> nav.navigate("guess_chat/$itemId") },
                        onOpenProfile = { nav.navigate(Routes.PROFILE) }
                    )
                }

                composable(Routes.ADD_ITEM) {
                    AddItemScreen(
                        itemId = null,  // Creating new item
                        onSave = { title ->
                            nav.previousBackStackEntry?.savedStateHandle?.set("newItem", title)
                            nav.popBackStack()
                        },
                        onBack = { nav.popBackStack() }
                    )
                }

                // ✅ EDIT ITEM ROUTE - Now properly enabled!
                composable(Routes.EDIT_ITEM_ROUTE) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId")
                    AddItemScreen(
                        itemId = itemId,  // Pass itemId to edit mode
                        onSave = { title ->
                            // Not used in edit mode, but keeping for consistency
                            nav.previousBackStackEntry?.savedStateHandle?.set("editedItem", title)
                            nav.popBackStack()
                        },
                        onBack = { nav.popBackStack() }
                    )
                }

                composable(Routes.HER_ITEMS) {
                    HerItemsScreen(
                        onBack = { nav.popBackStack() }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onBack = { nav.popBackStack() }
                    )
                }

                // ✅ GUESS CHAT ROUTE - Already properly set up
                composable(Routes.GUESS_CHAT_ROUTE) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId")
                    GuessChatScreen(
                        itemId = itemId,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}