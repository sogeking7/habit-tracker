package com.example.habit_tracker

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.convex.createClerkConvexClient
import dev.convex.android.ConvexClientWithAuth

/**
 * Initializes Clerk and the Convex client once, at process startup.
 *
 * [createClerkConvexClient] wires Clerk's session tokens into Convex, so
 * [convex] automatically sends the signed-in user's JWT with every request and
 * exposes the combined auth state via [ConvexClientWithAuth.authState].
 */
class HabitTrackerApplication : Application() {
    lateinit var convex: ConvexClientWithAuth<String>
        private set

    override fun onCreate() {
        super.onCreate()

        Clerk.initialize(
            context = this,
            publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY,
            options = ClerkConfigurationOptions(enableDebugMode = BuildConfig.DEBUG),
        )

        convex = createClerkConvexClient(
            deploymentUrl = BuildConfig.CONVEX_DEPLOYMENT_URL,
            context = applicationContext,
        )
    }
}
