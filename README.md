# SwapCampus — Notifications Exercise (UP)

## Goal
When a buyer reserves a listing, notify the listing owner in the app.

## Your task
Open `shared/src/commonMain/kotlin/com/isep/shared/data/InMemoryListingRepository.kt`.

Find the TODO inside `requestReservation()`. Create an `AppNotification` for the listing owner. Include the buyer's name and the listing title in the message.

For an example of creating a notification, look at `markListingSold()` in the same file.

## How to check your work
1. Run `androidApp` in Android Studio.
2. Log in as Bob: `bob@swapcampus.test` / `bob123`.
3. Reserve Alice's `Kotlin handbook`.
4. Log out without stopping the app.
5. Log in as Alice: `alice@swapcampus.test` / `alice123`.
6. Open Profile. An alert about Bob's reservation should appear.

Notifications are stored in memory. Restarting the app clears them.