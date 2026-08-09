package com.neonrush.game

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles anonymous auth + referral code storage in Firestore.
 *
 * Firestore structure:
 *   referralCodes/{code}  ->  { ownerId: "<uid>", createdAt: <timestamp> }
 *   players/{uid}         ->  { referredBy: "<code or null>", referralCredited: false }
 *
 * NOTE: This only handles code generation + lookup. Actually granting Gems to
 * both players when a referral converts requires a Cloud Function (needs Blaze
 * plan) so that the reward logic can't be spoofed from the client. That's the
 * next piece once billing is set up.
 */
class ReferralRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /** Call once on app start. Signs the player in anonymously if not already signed in. */
    suspend fun ensureSignedIn(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) return currentUser.uid

        val result = auth.signInAnonymously().await()
        return result.user?.uid
            ?: throw IllegalStateException("Anonymous sign-in failed to return a user")
    }

    /**
     * Returns this player's referral code, creating one in Firestore if it
     * doesn't exist yet. Safe to call every time the Share button is tapped.
     */
    suspend fun getOrCreateReferralCode(): String {
        val uid = ensureSignedIn()
        val playerRef = db.collection("players").document(uid)
        val snapshot = playerRef.get().await()

        val existingCode = snapshot.getString("referralCode")
        if (existingCode != null) return existingCode

        // Generate a short, readable code and make sure it's not already taken.
        var code: String
        do {
            code = (1..6)
                .map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }
                .joinToString("")
        } while (db.collection("referralCodes").document(code).get().await().exists())

        // Write both sides: the lookup entry, and the player's own record.
        db.collection("referralCodes").document(code)
            .set(mapOf("ownerId" to uid, "createdAt" to System.currentTimeMillis()))
            .await()

        playerRef.set(
            mapOf("referralCode" to code),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        return code
    }

    /**
     * Call this on first app launch if you have a referral code from an
     * install (e.g. from the Play Install Referrer API, once that's wired up).
     * Records who referred this player, but does NOT grant any reward here.
     */
    suspend fun recordIncomingReferral(code: String) {
        val uid = ensureSignedIn()
        val playerRef = db.collection("players").document(uid)
        val snapshot = playerRef.get().await()

        // Don't overwrite if this player already has a referredBy on file,
        // and never let a player refer themselves.
        val codeOwner = db.collection("referralCodes").document(code).get().await()
            .getString("ownerId")
        if (codeOwner == null || codeOwner == uid) return
        if (snapshot.getString("referredBy") != null) return

        playerRef.set(
            mapOf("referredBy" to code, "referralCredited" to false),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }
}
