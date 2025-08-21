package com.example.giftquest

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.giftquest.ui.screens.AddItemScreen
import com.example.giftquest.ui.screens.GuessChatScreen
import com.example.giftquest.ui.screens.HomeScreen
import com.example.giftquest.ui.screens.LoginScreen
import com.example.giftquest.ui.screens.SignUpScreen
import com.example.giftquest.ui.theme.GiftQuestTheme
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val ADD_ITEM = "add_item"
    const val GUESS_CHAT = "guess_chat"
    const val GUESS_CHAT_ROUTE = "guess_chat/{itemId}"
}


@Composable
fun GiftQuestApp() {
    GiftQuestTheme {
        val nav = rememberNavController()
        val isLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
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
                        onSkip = { nav.navigate(Routes.HOME) },
                        onCreateAccount = { nav.navigate(Routes.SIGN_UP) },   // NEW
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

                // ...
                composable(Routes.HOME) {
                    HomeScreen(
                        navController = nav,
                        onAddItem = { nav.navigate(Routes.ADD_ITEM) },
                        onOpenGuessChat = { itemId -> nav.navigate("guess_chat/$itemId") }
                    )
                }

                composable(Routes.ADD_ITEM) {
                    AddItemScreen(
                        onSave = { title ->
                            nav.previousBackStackEntry?.savedStateHandle?.set("newItem", title)
                            nav.popBackStack()
                        },
                        onBack = { nav.popBackStack() }
                    )
                }

// Accept itemId param
                composable(Routes.GUESS_CHAT_ROUTE) { backStackEntry ->
                    val itemId =
                        backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: -1L
                    com.example.giftquest.ui.screens.GuessChatScreen(
                        itemId = itemId,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
