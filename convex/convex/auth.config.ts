// Tells Convex which JWT issuer(s) to trust. Without this file,
// ctx.auth.getUserIdentity() always returns null.
//
// `domain` is the Clerk Frontend API URL (the JWT `iss` claim), e.g.
//   https://your-app-12.clerk.accounts.dev   (development)
//   https://clerk.your-domain.com             (production)
// Set it as a Convex environment variable rather than hardcoding:
//   npx convex env set CLERK_FRONTEND_API_URL https://your-app-12.clerk.accounts.dev
//
// `applicationID` must match the JWT `aud` claim. The Clerk → Convex
// integration issues tokens with an audience of "convex".
export default {
  providers: [
    {
      domain: process.env.CLERK_FRONTEND_API_URL,
      applicationID: "convex",
    },
  ],
};
