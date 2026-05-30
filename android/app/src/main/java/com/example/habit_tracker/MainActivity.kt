package com.example.habit_tracker

import android.Manifest.permission_group.PHONE
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.session.Session.SessionStatus
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userbutton.UserButton
import com.example.habit_tracker.ui.theme.HabitTrackerTheme
import dev.convex.android.AuthState
import dev.convex.android.ConvexClientWithAuth
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val convex = (application as HabitTrackerApplication).convex

        setContent {
            HabitTrackerTheme {
                // Combine Convex's auth state with Clerk's init/session state so we
                // don't flash the sign-in screen during the Clerk -> Convex handoff.
                val authState by remember(convex) {
                    combine(
                        convex.authState,
                        Clerk.isInitialized,
                        Clerk.sessionFlow,
                    ) { convexAuth, clerkInitialized, session ->
                        when (convexAuth) {
                            is AuthState.Authenticated -> AppAuthState.SignedIn
                            is AuthState.AuthLoading -> AppAuthState.Loading
                            is AuthState.Unauthenticated -> when {
                                !clerkInitialized -> AppAuthState.Loading
                                session?.status == SessionStatus.ACTIVE -> AppAuthState.Loading
                                else -> AppAuthState.SignedOut
                            }
                        }
                    }
                }.collectAsState(initial = AppAuthState.Loading)

                when (authState) {
                    AppAuthState.Loading -> LoadingScreen()
                    // Prebuilt Clerk sign-in/sign-up UI. The "Continue with Google"
                    // button appears automatically when Google is enabled in the
                    // Clerk dashboard.
                    AppAuthState.SignedOut -> AuthView()
                    AppAuthState.SignedIn -> SignedInApp(convex)
                }
            }
        }
    }
}

enum class AppAuthState { Loading, SignedIn, SignedOut }

@Composable
fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun SignedInApp(convex: ConvexClientWithAuth<String>) {
    // Mirror the Clerk identity into the Convex `users` table once signed in.
    LaunchedEffect(convex) {
        runCatching { convex.mutation("users:store") }
    }
    HabitTrackerApp(convex)
}

@Composable
fun HabitTrackerApp(convex: ConvexClientWithAuth<String>) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = stringResource(it.label)
                        )
                    },
                    label = { Text(stringResource(it.label)) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        navigationSuiteColors = NavigationSuiteDefaults.colors()
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeDestination()
                    AppDestinations.PROFILE -> ProfileDestination(convex)
                    AppDestinations.FAVORITES -> FavoritesDestination()
                }
            }
        }
    }
}

enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.home, Icons.Default.Home),
    FAVORITES(R.string.favorites, Icons.Default.Favorite),
    PROFILE(R.string.profile, Icons.Default.AccountBox),
}

@Preview(name = "Phone", device = PHONE, showSystemUi = true)
@Composable
fun HomeDestination() {
    Surface {
        Text(text = stringResource(R.string.home))
    }
}

/**
 * The shape of a `users` document returned by the Convex `users:current` query.
 * Unknown fields (e.g. `_creationTime`) are ignored by the client's JSON decoder.
 */
@Serializable
data class ConvexUser(
    @SerialName("_id") val id: String,
    val tokenIdentifier: String,
    val name: String? = null,
    val email: String? = null,
    val pictureUrl: String? = null,
)

@Composable
fun ProfileDestination(convex: ConvexClientWithAuth<String>) {
    // Read the signed-in user back from Convex. Subscribing with a nullable type
    // means the flow survives the brief window before `users:store` runs (the
    // query returns null until then) instead of being canceled.
    val user by produceState<ConvexUser?>(initialValue = null, convex) {
        convex.subscribe<ConvexUser?>("users:current").collect { result ->
            result.onSuccess { value = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Prebuilt Clerk avatar that opens the profile + sign-out menu.
        UserButton()
        Text(text = user?.let { it.name ?: it.email ?: "Signed in" } ?: "Syncing your account…")
        user?.email?.let { Text(text = it) }
    }
}

@Composable
fun FavoritesDestination() {
    Surface {
        Text(text = stringResource(R.string.favorites))
    }
}
