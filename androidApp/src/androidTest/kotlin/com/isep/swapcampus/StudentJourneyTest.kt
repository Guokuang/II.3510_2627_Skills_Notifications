package com.isep.swapcampus

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatDelegate
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the lab's main student flow against the real in-memory repositories. */
@RunWith(AndroidJUnit4::class)
class StudentJourneyTest {
    @get:Rule(order = 0) val nightMode = object : TestWatcher() {
        override fun starting(description: Description) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppCompatDelegate.setDefaultNightMode(
                    if (description.methodName == "darkModeRemainsReadable") AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        override fun finished(description: Description) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun click(text: String) {
        val node = compose.onAllNodes(hasText(text) and hasClickAction()).onLast()
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }
    private fun login(email: String, password: String) {
        compose.onNodeWithTag("login-email").performTextReplacement(email)
        compose.onNodeWithTag("login-password").performTextReplacement(password)
        compose.onNode(hasText("Log in") and hasClickAction()).performClick()
        compose.waitForIdle()
    }
    private fun back() = compose.onNodeWithContentDescription("Back").performClick()
    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.waitForIdle(200, 2000)
        val target = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        target.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
        val mkdir = instrumentation.uiAutomation.executeShellCommand("mkdir -p /data/local/tmp/swapcampus-labwork")
        ParcelFileDescriptor.AutoCloseInputStream(mkdir).use { it.readBytes() }
        val copy = instrumentation.uiAutomation.executeShellCommand("cp ${target.absolutePath} /data/local/tmp/swapcampus-labwork/capture-$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
    }
    private fun switchAccount(email: String, password: String) {
        click("Profile")
        click("Log out")
        login(email, password)
    }

    @Test fun guestLoginPostEditReserveAndTranslate() {
        compose.onNodeWithText("Kotlin handbook").assertIsDisplayed()
        compose.onNodeWithText("€12.50").assertIsDisplayed()
        screenshot("01-listings-en")
        compose.onNode(hasText("Furniture") and hasAnyAncestor(hasTestTag("category-chips"))).performScrollTo().performClick()
        compose.onNodeWithText("Desk lamp").assertIsDisplayed()
        compose.onNodeWithText("Kotlin handbook").assertDoesNotExist()
        compose.onNodeWithTag("category-chips").performSemanticsAction(SemanticsActions.ScrollBy) { it(-10000f, 0f) }
        compose.onNode(hasText("All") and hasAnyAncestor(hasTestTag("category-chips"))).performClick()
        // Search and empty state.
        compose.onNode(hasSetTextAction()).performTextInput("no-such-listing")
        compose.onNodeWithText("No listings found.").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextClearance()
        click("Kotlin handbook")
        click("Reserve")
        // Invalid credentials keep the login screen open.
        login("bob@swapcampus.test", "wrong")
        compose.onNodeWithText("Wrong email or password.").assertIsDisplayed()
        login("bob@swapcampus.test", "bob123")
        compose.onNodeWithText("Kotlin handbook").assertIsDisplayed()
        click("Reserve")
        compose.onNodeWithText("Reserved", substring = false).assertIsDisplayed()
        back()
        switchAccount("alice@swapcampus.test", "alice123")
        click("My listings")
        click("Kotlin handbook")
        click("Cancel reservation")
        compose.onNodeWithText("Available").assertIsDisplayed()
        back()
        switchAccount("bob@swapcampus.test", "bob123")
        click("Listings")
        click("Kotlin handbook")
        click("Reserve")
        back()
        switchAccount("alice@swapcampus.test", "alice123")
        click("My listings")
        click("Kotlin handbook")
        click("Mark as sold")
        compose.onNodeWithText("Sold").assertIsDisplayed()
        click("Make available again")
        compose.onNodeWithText("Available").assertIsDisplayed()
        back()
        switchAccount("bob@swapcampus.test", "bob123")
        click("Listings")
        // Post validation, creation and editing.
        compose.onNodeWithContentDescription("Post a listing").performClick()
        click("Post")
        compose.onNodeWithText("Title is required.").assertIsDisplayed()
        compose.onNodeWithTag("listing-title").performTextReplacement("Journey test book")
        compose.onNodeWithTag("listing-price").performTextReplacement("abc")
        click("Post")
        compose.onNodeWithText("Enter a finite price greater than 0.").assertIsDisplayed()
        compose.onNodeWithTag("listing-price").performTextReplacement("18,50")
        click("Post")
        click("My listings")
        click("Journey test book")
        click("Edit")
        compose.onNodeWithTag("listing-title").performTextReplacement("Edited journey book")
        click("Save")
        compose.onNodeWithText("Edited journey book").assertIsDisplayed()
        // Reserving one's own listing must explain the error.
        click("Reserve")
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("You cannot reserve your own listing or choose an invalid buyer.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("You cannot reserve your own listing or choose an invalid buyer.").assertExists()
        click("Delete")
        click("Cancel")
        compose.onNodeWithText("Edited journey book").assertExists()
        click("Delete")
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Edited journey book").assertDoesNotExist()
        click("Profile")
        compose.onNodeWithText("Email: bob@swapcampus.test").assertIsDisplayed()
        click("French")
        compose.onAllNodesWithText("Profil").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Se déconnecter").assertIsDisplayed()
        compose.onNodeWithText("Membre depuis le", substring = true).assertIsDisplayed()
        compose.onNodeWithText("oct.", substring = true).assertIsDisplayed()
        screenshot("02-profile-fr")
        click("Annonces")
        compose.onNodeWithText("Kotlin handbook").assertIsDisplayed()
        compose.onNodeWithText("12,50", substring = true).assertIsDisplayed()
        screenshot("03-listings-fr")
        click("Profil")
        click("Anglais")
        click("Log out")
        compose.onNodeWithTag("login-email").assertIsDisplayed()
    }

    @Test fun darkModeRemainsReadable() {
        compose.onNodeWithText("Kotlin handbook").assertIsDisplayed()
        screenshot("04-listings-dark")
        click("Profile")
        compose.onNodeWithTag("login-email").assertIsDisplayed()
        compose.onNodeWithTag("login-password").assertIsDisplayed()
        screenshot("05-login-dark")
    }
}
