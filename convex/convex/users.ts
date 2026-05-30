import { v } from "convex/values";
import {
  query,
  mutation,
  QueryCtx,
  MutationCtx,
} from "./_generated/server";
import { Doc } from "./_generated/dataModel";

// Look up the signed-in user's document, or null if not signed in / not yet
// stored. Use this inside other queries/mutations to scope data to a user.
export async function getCurrentUser(
  ctx: QueryCtx | MutationCtx,
): Promise<Doc<"users"> | null> {
  const identity = await ctx.auth.getUserIdentity();
  if (identity === null) {
    return null;
  }
  return await ctx.db
    .query("users")
    .withIndex("by_token", (q) =>
      q.eq("tokenIdentifier", identity.tokenIdentifier),
    )
    .unique();
}

// Same as getCurrentUser, but throws when unauthenticated. Use this in
// functions that must only run for a signed-in user.
export async function requireCurrentUser(
  ctx: QueryCtx | MutationCtx,
): Promise<Doc<"users">> {
  const user = await getCurrentUser(ctx);
  if (user === null) {
    throw new Error("Not authenticated");
  }
  return user;
}

// Returns the signed-in user's profile (or null). The Android client
// subscribes to this to render the profile screen.
export const current = query({
  args: {},
  handler: async (ctx) => {
    return await getCurrentUser(ctx);
  },
});

// Upsert the signed-in user into the `users` table. Call this from the client
// right after sign-in so the database has a row mirroring the Clerk identity.
export const store = mutation({
  args: {},
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (identity === null) {
      throw new Error("Called users:store without authentication");
    }

    const existing = await ctx.db
      .query("users")
      .withIndex("by_token", (q) =>
        q.eq("tokenIdentifier", identity.tokenIdentifier),
      )
      .unique();

    if (existing !== null) {
      // Keep the mirrored profile fresh, but only write when something changed.
      if (
        existing.name !== identity.name ||
        existing.email !== identity.email ||
        existing.pictureUrl !== identity.pictureUrl
      ) {
        await ctx.db.patch(existing._id, {
          name: identity.name,
          email: identity.email,
          pictureUrl: identity.pictureUrl,
        });
      }
      return existing._id;
    }

    return await ctx.db.insert("users", {
      tokenIdentifier: identity.tokenIdentifier,
      name: identity.name,
      email: identity.email,
      pictureUrl: identity.pictureUrl,
    });
  },
});
