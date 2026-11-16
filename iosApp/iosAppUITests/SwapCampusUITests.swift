import XCTest

@MainActor
final class SwapCampusUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
    }

    private func launch() {
        app.launch()
        assertText("Kotlin handbook")
    }

    private func candidates(_ label: String) -> [XCUIElement] {
        let exact = NSPredicate(format: "label == %@", label)
        let partial = NSPredicate(format: "label CONTAINS %@", label)
        return [app.buttons.matching(exact).firstMatch,
                app.staticTexts.matching(exact).firstMatch,
                app.buttons.matching(partial).firstMatch,
                app.descendants(matching: .any).matching(partial).firstMatch]
    }

    private func find(_ label: String) -> XCUIElement {
        for _ in 0..<30 {
            for item in candidates(label) where item.exists { return item }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
        XCTFail("Missing label: \(label)\n\(app.debugDescription)")
        return app
    }

    private func assertText(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(find(text).exists, "Missing \(text)", file: file, line: line)
    }

    private func visibleBottom() -> CGFloat {
        let keyboard = app.keyboards.firstMatch
        return keyboard.exists ? keyboard.frame.minY - 60 : app.frame.height - 40
    }

    private func scrollForm(down: Bool = false) {
        let height = app.frame.height
        let upper: CGFloat = 155
        let lower = max(upper + 80, visibleBottom() - 15)
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: (down ? upper : lower) / height))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: (down ? lower : upper) / height))
        start.press(forDuration: 0.05, thenDragTo: end)
    }

    private func tap(_ text: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.4))
        let exact = NSPredicate(format: "label == %@", text)
        for _ in 0..<7 {
            let targets = [app.buttons.matching(exact).firstMatch, app.staticTexts.matching(exact).firstMatch]
            for item in targets where item.exists {
                if item.isHittable && item.frame.midY < visibleBottom() {
                    item.tap()
                    RunLoop.current.run(until: Date().addingTimeInterval(0.7))
                    return
                }
            }
            scrollForm()
        }
        XCTFail("Exact label is not visible and hittable: \(text)\n\(app.debugDescription)")
    }

    private func input(_ tag: String, placeholder: String, secure: Bool = false) -> XCUIElement {
        let tagged = app.descendants(matching: .any).matching(identifier: tag).firstMatch
        if tagged.exists { return tagged }
        if secure {
            let field = app.secureTextFields.firstMatch
            if field.exists { return field }
        }
        let named = app.textFields.matching(NSPredicate(format: "label CONTAINS %@ OR placeholderValue CONTAINS %@", placeholder, placeholder)).firstMatch
        if named.exists { return named }
        // Compose text inputs appear as native TextView accessibility elements.
        let textView = app.textViews.matching(NSPredicate(format: "label CONTAINS %@", placeholder)).firstMatch
        if textView.exists { return textView }
        XCTFail("Input not found: \(tag)\n\(app.debugDescription)")
        return app
    }

    private func fill(_ tag: String, placeholder: String, value: String, secure: Bool = false) {
        let field = input(tag, placeholder: placeholder, secure: secure)
        for _ in 0..<5 {
            if field.isHittable && field.frame.midY > 140 && field.frame.midY < visibleBottom() { break }
            scrollForm(down: field.frame.midY < 140)
        }
        field.tap()
        let previous = field.value as? String ?? ""
        if !previous.isEmpty && previous != placeholder {
            app.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: previous.count))
        }
        if !value.isEmpty { app.typeText(value) }
        if tag.hasPrefix("listing-") || tag == "search" {
            let predicate = NSPredicate(format: "label ==[c] %@ OR label ==[c] %@", "done", "search")
            let action = app.keyboards.descendants(matching: .any).matching(predicate).firstMatch
            XCTAssertTrue(action.waitForExistence(timeout: 3), "Keyboard action missing for \(tag)")
            action.tap()
            RunLoop.current.run(until: Date().addingTimeInterval(0.7))
        }
    }

    private func login(_ account: String, password: String? = nil) {
        fill("login-email", placeholder: "Email", value: "\(account)@swapcampus.test")
        fill("login-password", placeholder: "Password", value: password ?? "\(account)123", secure: true)
        tap("Log in")
    }

    private func switchAccount(_ account: String) {
        tap("Profile")
        tap("Log out")
        login(account)
    }

    private func back() { tap("Back") }

    private func capture(_ name: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.8))
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testGuestBrowseAndLoginReturn() {
        launch()
        assertText("12.50")
        capture("English listings")
        fill("search", placeholder: "Search listings", value: "no-such-listing")
        assertText("No listings found.")
        fill("search", placeholder: "Search listings", value: "")
        tap("Books")
        assertText("Kotlin handbook")
        tap("All")
        tap("Kotlin handbook")
        assertText("Seller: Alice")
        assertText("Posted on")
        back()
        tap("Kotlin handbook")
        tap("Reserve")
        login("bob", password: "wrong")
        assertText("Wrong email or password.")
        fill("login-password", placeholder: "Password", value: "bob123", secure: true)
        tap("Log in")
        assertText("Kotlin handbook")
        tap("Reserve")
        assertText("Reserved")
        capture("Reserved listing after login")
        back()
        tap("Profile")
        assertText("bob@swapcampus.test")
        assertText("Member since")
        assertText("Change the language in the iPhone Settings.")
        tap("Log out")
        assertText("Demo accounts")
    }

    func testPostEditAndDelete() {
        launch()
        tap("Profile")
        login("alice")
        tap("Listings")
        tap("Post a listing")
        tap("Post")
        assertText("Title is required.")
        fill("listing-title", placeholder: "Title", value: "iPhone test listing")
        fill("listing-price", placeholder: "Price", value: "abc")
        tap("Post")
        assertText("Enter a finite price greater than 0.")
        fill("listing-price", placeholder: "Price", value: "18,50")
        tap("Post")
        tap("My listings")
        tap("iPhone test listing")
        tap("Edit")
        fill("listing-title", placeholder: "Title", value: "Edited iPhone listing")
        tap("Save")
        assertText("Edited iPhone listing")
        tap("Reserve")
        assertText("You cannot reserve your own listing")
        tap("Delete")
        tap("Cancel")
        assertText("Edited iPhone listing")
        tap("Delete")
        let dialog = app.alerts.firstMatch
        if dialog.exists { dialog.buttons["Delete"].tap() }
        else { app.buttons.matching(NSPredicate(format: "label == %@", "Delete")).allElementsBoundByIndex.last!.tap() }
        assertText("My listings")
        capture("My listings after deletion")
    }

    func testReservationStatusLifecycle() {
        launch()
        tap("Kotlin handbook")
        tap("Reserve")
        login("bob")
        tap("Reserve")
        back()
        switchAccount("alice")
        tap("My listings")
        tap("Kotlin handbook")
        tap("Cancel reservation")
        assertText("Available")
        back()
        switchAccount("bob")
        tap("Listings")
        tap("Kotlin handbook")
        tap("Reserve")
        back()
        switchAccount("alice")
        tap("My listings")
        tap("Kotlin handbook")
        tap("Mark as sold")
        assertText("Sold")
        tap("Make available again")
        assertText("Available")
        capture("Seller restores availability")
    }

    func testFrenchResources() {
        app.launchArguments = ["-AppleLanguages", "(fr)", "-AppleLocale", "fr_FR"]
        app.launch()
        assertText("Annonces")
        assertText("12,50")
        capture("French listings")
        tap("Profil")
        fill("login-email", placeholder: "E-mail", value: "alice@swapcampus.test")
        fill("login-password", placeholder: "Mot de passe", value: "alice123", secure: true)
        tap("Se connecter")
        assertText("Membre depuis le")
        assertText("Réglages")
        capture("French profile")
    }

    func testDarkModeReadable() {
        launch()
        capture("Dark listings")
        tap("Profile")
        assertText("Email")
        assertText("Password")
        capture("Dark login")
    }

    func testEdgeSwipeBack() {
        launch()
        tap("Kotlin handbook")
        assertText("Seller: Alice")
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.005, dy: 0.5))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: end)
        XCTAssertTrue(app.buttons["Profile"].waitForExistence(timeout: 8), "Edge swipe must restore the list tabs")
        XCTAssertFalse(app.buttons["Back"].exists, "Detail must leave the navigation stack")
        assertText("Kotlin handbook")
        capture("Edge swipe restores listings")
    }

    func testLanguageThroughSystemSettings() {
        // No AppleLanguages/AppleLocale overrides: exercise the OS app preference.
        app.launchArguments = []
        app.launch()
        assertText("Listings")
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        settings.launch()

        func scrollTo(_ text: String) -> XCUIElement {
            let element = settings.staticTexts[text].firstMatch
            for _ in 0..<22 {
                if element.exists && element.isHittable { return element }
                settings.swipeUp()
            }
            XCTFail("Settings item missing: \(text)\n\(settings.debugDescription)")
            return element
        }

        scrollTo("Apps").tap()
        scrollTo("SwapCampus").tap()
        let language = settings.staticTexts["Language"].firstMatch
        XCTAssertTrue(language.waitForExistence(timeout: 5), "App language must be offered by iOS Settings")
        language.tap()
        let french = settings.staticTexts["Français"].firstMatch
        XCTAssertTrue(french.waitForExistence(timeout: 5), "French language option missing")
        french.tap()
        // iOS may display a confirmation sheet after a language selection.
        let confirmation = settings.alerts.buttons.matching(NSPredicate(format: "label CONTAINS %@", "French")).firstMatch
        if confirmation.waitForExistence(timeout: 2) { confirmation.tap() }
        app.terminate()
        app.launch()
        assertText("Annonces")
        assertText("12,50")
        capture("French after change in actual iOS Settings")

        settings.activate()
        if !settings.staticTexts["English"].firstMatch.exists {
            let entry = settings.staticTexts["Language"].firstMatch
            XCTAssertTrue(entry.waitForExistence(timeout: 5))
            entry.tap()
        }
        let english = settings.staticTexts["English"].firstMatch
        XCTAssertTrue(english.waitForExistence(timeout: 5))
        english.tap()
        let englishConfirmation = settings.alerts.buttons.matching(NSPredicate(format: "label CONTAINS %@", "English")).firstMatch
        if englishConfirmation.waitForExistence(timeout: 2) { englishConfirmation.tap() }
        app.terminate()
        app.launch()
        assertText("Listings")
        assertText("12.50")
        capture("English restored through iOS Settings")
    }

}
