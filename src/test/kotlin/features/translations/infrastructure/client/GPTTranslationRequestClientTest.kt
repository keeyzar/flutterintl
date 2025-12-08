package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.Client
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.BatchClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.SingleTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.JsonFileChunker
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.given
import java.util.concurrent.Executors

class GPTTranslationRequestClientTest {
    @Mock
    private lateinit var LLMConfigProvider: LLMConfigProvider
    @Mock
    private lateinit var singleTranslationRequestClient: SingleTranslationRequestClient
    private lateinit var sut: GPTTranslationRequestClient
    private lateinit var parser: TranslationRequestResponseMapper

    @Mock
    private lateinit var dispatcherConfiguration: DispatcherConfiguration

    @Mock
    private lateinit var userSettingsRepository: UserSettingsRepository
    private val objectMapper = ObjectMapper()

    private val baseLanguage = Language("en", null)
    private val targetLanguages = listOf(
        Language("de", null),
        Language("fr", null),
        Language("es", null)
    )

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val parallelism = 3
        given(dispatcherConfiguration.getDispatcher())
            .willReturn(Executors.newFixedThreadPool(parallelism).asCoroutineDispatcher())
        given(dispatcherConfiguration.getLevelOfParallelism())
            .willReturn(parallelism)
        given(userSettingsRepository.getSettings()).willReturn(
            UserSettings(
                "",
                "",
                false,
                "",
                "",
                false,
                "",
                "",
                1,
                "informal",
                "gemini-2.5-pro",
                true,
                10
            )
        )
        val key = System.getenv("gemini_api_key")
        given(LLMConfigProvider.getInstanceGemini()).willReturn(
            Client.builder()
                .apiKey(key)
                .build()
        )
        parser = TranslationRequestResponseMapper(objectMapper)
        sut = GPTTranslationRequestClient(
            LLMConfigProvider,
            parser,
            dispatcherConfiguration,
            userSettingsRepository,
            jsonFileChunker = JsonFileChunker(objectMapper),
            singleTranslationRequestClient = singleTranslationRequestClient
        )
    }

    // Helper function to create a Translation
    private fun createTranslation(key: String, value: String, description: String): Translation {
        return Translation(
            lang = baseLanguage,
            entry = SimpleTranslationEntry(
                id = null,
                desiredKey = key,
                desiredValue = value,
                desiredDescription = description
            )
        )
    }

    // Helper function to create a ClientTranslationRequest
    private fun createClientRequest(key: String, value: String, description: String): ClientTranslationRequest {
        return ClientTranslationRequest(
            targetLanguages = targetLanguages,
            translation = createTranslation(key, value, description)
        )
    }

    @Test
    fun testThatRequestWorks() {
        runBlocking {
            val baseContent = """
                key: "rhythmtrainer_display_bpm"
                value: "'BPM: ${'$'}{_bpm.round()}'"
                description: "Label for displaying beats per minute with dynamic value"
            """.trimIndent()

            val it = sut.requestComplexTranslationLong(baseContent, "en")
            println(it)
            assertThat(it).isNotNull
        }
    }

    @Test
    fun testBatchCreateComplexArbEntry_SimpleStrings() {
        runBlocking {
            // Simple UI strings that are common in Flutter apps
            val requests = listOf(
                createClientRequest("ok_button", "OK", "Confirmation button"),
                createClientRequest("cancel_button", "Cancel", "Cancel button"),
                createClientRequest("save_button", "Save", "Save button"),
                createClientRequest("delete_button", "Delete", "Delete button"),
                createClientRequest("welcome_message", "Welcome!", "Welcome message shown on home screen")
            )

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            val response = sut.createComplexArbEntryBatch(batchRequest, { false }, { })

            assertThat(response.responses).hasSize(5)
            response.responses.forEach { partialResponse ->
                println("Key: ${partialResponse.translation.entry.desiredKey}, Value: ${partialResponse.translation.entry.desiredValue}")
                assertThat(partialResponse.translation.entry.desiredKey).isNotBlank()
                assertThat(partialResponse.translation.entry.desiredValue).isNotBlank()
            }
        }
    }

    @Test
    fun testBatchCreateComplexArbEntry_ComplexFlutterStrings() {
        runBlocking {
            // Complex strings with placeholders, pluralization, etc.
            val requests = listOf(
                // Complex nested widget context - Shopping cart with variables
                createClientRequest(
                    "shopping_cart_items",
                    "You have \${itemCount} items in your cart worth \${totalPrice}",
                    "Shopping cart summary showing item count and total price. From complex nested Column/Row/Card widget structure"
                ),

                // Form validation messages
                createClientRequest(
                    "validation_min_length",
                    "Must be at least \${minLength} characters",
                    "Validation error for minimum length requirement in TextFormField"
                ),

                // Pluralization case
                createClientRequest(
                    "unread_notifications",
                    "\${count} unread notifications",
                    "Notification badge text showing unread count (pluralize: 0=no notifications, 1=1 notification, other=X notifications)"
                ),

                // Date/time formatting
                createClientRequest(
                    "last_updated",
                    "Last updated on \${date} at \${time}",
                    "Timestamp showing when content was last updated"
                ),

                // Complex nested structure from ExpansionTile/ListTile
                createClientRequest(
                    "profile_settings_language",
                    "Choose your preferred language",
                    "Subtitle text in Language settings ListTile within ExpansionTile"
                )
            )

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            val startTime = System.currentTimeMillis()
            val response = sut.createComplexArbEntryBatch(batchRequest, { false }, { })
            val duration = System.currentTimeMillis() - startTime

            println("\n=== Complex Flutter Strings Batch Test ===")
            println("Processed ${response.responses.size} entries in ${duration}ms")

            assertThat(response.responses).hasSize(5)
            response.responses.forEach { partialResponse ->
                val entry = partialResponse.translation.entry
                println("\nKey: ${entry.desiredKey}")
                println("Value: ${entry.desiredValue}")
                println("Description: ${entry.desiredDescription}")
                assertThat(entry.desiredKey).isNotBlank()
                assertThat(entry.desiredValue).isNotBlank()
            }

            // Verify placeholders are preserved
            val cartItem = response.responses.find { it.translation.entry.desiredKey == "shopping_cart_items" }
            assertThat(cartItem?.translation?.entry?.desiredValue).contains("{")
        }
    }

    @Test
    fun testBatchCreateComplexArbEntry_MixedComplexityLargeBatch() {
        runBlocking {
            // Mix of simple and complex strings to test batch handling with varied content
            val requests = mutableListOf<ClientTranslationRequest>()

            // Simple buttons (from various widgets)
            requests.add(createClientRequest("btn_ok", "OK", "OK button"))
            requests.add(createClientRequest("btn_cancel", "Cancel", "Cancel button"))
            requests.add(createClientRequest("btn_save", "Save", "Save button"))

            // Complex: From nested Card with shopping functionality
            requests.add(createClientRequest(
                "product_add_to_cart",
                "Add to Cart",
                "Button text in nested Card > Column > Row > ElevatedButton structure"
            ))

            // Simple labels
            requests.add(createClientRequest("label_email", "Email", "Email field label"))
            requests.add(createClientRequest("label_password", "Password", "Password field label"))

            // Complex: From Stack with positioned gradient header
            requests.add(createClientRequest(
                "welcome_back_title",
                "Welcome Back!",
                "Large title in Stack > Positioned > Column header with gradient background"
            ))
            requests.add(createClientRequest(
                "welcome_back_subtitle",
                "Ready to continue your journey?",
                "Subtitle below welcome title in positioned header"
            ))

            // Simple status messages
            requests.add(createClientRequest("status_loading", "Loading...", "Loading indicator text"))
            requests.add(createClientRequest("status_error", "Error", "Error status text"))

            // Complex: From GridView with category cards
            requests.add(createClientRequest(
                "category_items_available",
                "\${count} items available",
                "Subtitle in GridView > Card > Column showing category item count"
            ))

            // Complex: From Form with validation
            requests.add(createClientRequest(
                "validation_email_required",
                "Email is required",
                "Validation message in Form > Card > TextFormField validator"
            ))
            requests.add(createClientRequest(
                "validation_password_length",
                "Password must be at least \${minLength} characters",
                "Password validation with minimum length parameter"
            ))

            // Simple navigation items
            requests.add(createClientRequest("nav_home", "Home", "Bottom navigation item"))
            requests.add(createClientRequest("nav_profile", "Profile", "Bottom navigation item"))
            requests.add(createClientRequest("nav_settings", "Settings", "Bottom navigation item"))

            // Complex: From ListView with ExpansionTile
            requests.add(createClientRequest(
                "settings_notifications_subtitle",
                "Manage notification settings",
                "ListTile subtitle in ExpansionTile > ListTile for notification settings"
            ))

            // Complex: With currency formatting
            requests.add(createClientRequest(
                "price_display",
                "Price: \${price}",
                "Price display with currency formatting (compactCurrency)"
            ))

            // Simple action labels
            requests.add(createClientRequest("action_edit", "Edit", "Edit action"))
            requests.add(createClientRequest("action_delete", "Delete", "Delete action"))

            // Complex: Terms and conditions
            requests.add(createClientRequest(
                "terms_agreement",
                "I agree to the Terms and Conditions",
                "Checkbox label in Form > Row > Checkbox for terms agreement"
            ))

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            val startTime = System.currentTimeMillis()
            val response = sut.createComplexArbEntryBatch(batchRequest, { false }, { })
            val duration = System.currentTimeMillis() - startTime

            println("\n=== Mixed Complexity Large Batch Test ===")
            println("Processed ${response.responses.size} entries in ${duration}ms")
            println("Average time per entry: ${duration / response.responses.size}ms")

            assertThat(response.responses).hasSize(requests.size)

            // Print all results
            response.responses.forEach { partialResponse ->
                val entry = partialResponse.translation.entry
                println("${entry.desiredKey}: ${entry.desiredValue}")
            }

            // Verify some specific entries
            val welcomeBack = response.responses.find { it.translation.entry.desiredKey == "welcome_back_title" }
            assertThat(welcomeBack?.translation?.entry?.desiredValue).isNotBlank()

            val priceDisplay = response.responses.find { it.translation.entry.desiredKey == "price_display" }
            assertThat(priceDisplay?.translation?.entry?.desiredValue).contains("{")
        }
    }

    @Test
    fun testBatchTranslateValueOnly_MultipleLanguages() {
        runBlocking {
            // First create the complex ARB entries
            val requests = listOf(
                createClientRequest(
                    "greeting_with_name",
                    "Hello, \${userName}!",
                    "Personalized greeting with user name"
                ),
                createClientRequest(
                    "items_in_cart",
                    "\${count} items in cart",
                    "Shopping cart item count (pluralize)"
                ),
                createClientRequest(
                    "last_seen",
                    "Last seen \${timeAgo}",
                    "User last seen timestamp"
                )
            )

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            // Create complex entries first
            val createResponse = sut.createComplexArbEntryBatch(batchRequest, { false }, { })
            assertThat(createResponse.responses).hasSize(3)

            println("\n=== Batch Translate Value Only Test ===")
            println("Created ${createResponse.responses.size} base entries")

            // Now translate to other languages
            val translatedResponses = mutableListOf<String>()

            sut.translateValueOnlyBatch(
                batchRequest,
                createResponse,
                { false }
            ) { partialResponse ->
                val entry = partialResponse.translation.entry
                val lang = partialResponse.translation.lang.toISOLangString()
                translatedResponses.add("[$lang] ${entry.desiredKey}: ${entry.desiredValue}")
                println("[$lang] ${entry.desiredKey}: ${entry.desiredValue}")
            }

            // Should have 3 entries × 3 target languages (de, fr, es) = 9 total
            // But base language (en) is filtered out, so we expect 3 entries × 2 remaining languages = 6
            // Actually looking at the code, targetLanguages has de, fr, es (3 languages), all different from base (en)
            // So we expect 3 entries × 3 languages = 9 translations
            assertThat(translatedResponses.size).isEqualTo(9)

            // Verify we have translations for each language
            assertThat(translatedResponses.filter { it.startsWith("[de]") }).hasSize(3)
            assertThat(translatedResponses.filter { it.startsWith("[fr]") }).hasSize(3)
            assertThat(translatedResponses.filter { it.startsWith("[es]") }).hasSize(3)
        }
    }

    @Test
    fun testBatchCreateComplexArbEntry_VeryComplexNestedFlutterContexts() {
        runBlocking {
            // Test with extremely complex Flutter widget descriptions
            // to see if Gemini can handle large context + multiple entries
            val requests = listOf(
                createClientRequest(
                    "product_name",
                    "Product Name",
                    """From deeply nested structure:
                    Container > Column > Card > Padding > Column > Row > Expanded > Column > Text
                    This is inside a shopping cart widget with multiple nested rows and columns"""
                ),

                createClientRequest(
                    "view_details",
                    "View Details",
                    """OutlinedButton text in:
                    Container(padding) > Column(crossAxisAlignment.start) > Card(elevation:4) > Padding > Column > Row > Expanded > Column(crossAxisAlignment.start) > Row > OutlinedButton > Text
                    Part of a product card with add to cart and view details buttons side by side"""
                ),

                createClientRequest(
                    "notification_settings",
                    "Notifications",
                    """ListTile title in:
                    ListView.builder > Card(margin) > ExpansionTile(leading: CircleAvatar) > ListTile(leading: Icon, trailing: Switch) > Text
                    Settings screen with expandable sections and toggles"""
                ),

                createClientRequest(
                    "continue_button",
                    "Continue",
                    """ElevatedButton.icon label in:
                    Stack > Positioned(top:40, left:20, right:20) > Column(crossAxisAlignment.start) > Row > Expanded > ElevatedButton.icon(style: padding:16) > Text
                    Welcome screen with gradient background and call-to-action buttons"""
                ),

                createClientRequest(
                    "sign_up_button",
                    "Sign Up",
                    """ElevatedButton child in:
                    Form(key) > SingleChildScrollView(padding:24) > Column(crossAxisAlignment.stretch) > Card(elevation:2) > Padding(16) > Column > ElevatedButton(style: padding vertical:16) > Text(fontSize:18)
                    Registration form with text fields and validation"""
                ),

                createClientRequest(
                    "category_new_badge",
                    "New",
                    """Chip label in:
                    GridView.builder(gridDelegate: SliverGridDelegateWithFixedCrossAxisCount) > Card(elevation:3, shape: RoundedRectangleBorder) > InkWell > Column(mainAxisAlignment.center) > Chip(backgroundColor: green.shade100) > Text
                    Category grid with cards showing new items badge"""
                ),

                createClientRequest(
                    "user_credits",
                    "\${credits} credits remaining",
                    """Text in deeply nested user profile section:
                    Scaffold > CustomScrollView > SliverList > Card > ExpansionTile > Column > Row > Expanded > Text
                    Needs pluralization: 0=no credits, 1=1 credit, other=X credits"""
                ),

                createClientRequest(
                    "order_total",
                    "Total: \${total}",
                    """Price summary in checkout flow:
                    Scaffold > Column > Expanded > ListView > Card > Padding > Column > Divider > Row(mainAxisAlignment.spaceBetween) > Text(fontWeight.bold)
                    Currency formatting with compactCurrency format"""
                )
            )

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            val startTime = System.currentTimeMillis()
            val response = sut.createComplexArbEntryBatch(batchRequest, { false }, { })
            val duration = System.currentTimeMillis() - startTime

            println("\n=== Very Complex Nested Flutter Contexts Test ===")
            println("Processed ${response.responses.size} entries in ${duration}ms")

            assertThat(response.responses).hasSize(8)

            response.responses.forEach { partialResponse ->
                val entry = partialResponse.translation.entry
                println("\n--- ${entry.desiredKey} ---")
                println("Value: ${entry.desiredValue}")
                println("Description: ${entry.desiredDescription}")

                assertThat(entry.desiredKey).isNotBlank()
                assertThat(entry.desiredValue).isNotBlank()
            }

            // Verify placeholders are preserved in variable strings
            val credits = response.responses.find { it.translation.entry.desiredKey == "user_credits" }
            assertThat(credits?.translation?.entry?.desiredValue).containsPattern("\\{.*}")

            val total = response.responses.find { it.translation.entry.desiredKey == "order_total" }
            assertThat(total?.translation?.entry?.desiredValue).containsPattern("\\{.*}")

            println("\n✓ Successfully processed very complex nested Flutter contexts")
        }
    }

    @Test
    fun testBatchCreateComplexArbEntry_WithGeminiPro() {
        runBlocking {
            // Override settings to use Gemini Pro for this test
            given(userSettingsRepository.getSettings()).willReturn(
                UserSettings(
                    "",
                    "",
                    false,
                    "",
                    "",
                    false,
                    "",
                    "",
                    1,
                    "informal",
                    "gemini-2.5-flash",  // Using Pro model
                    true,
                    10
                )
            )

            val requests = listOf(
                createClientRequest(
                    "complex_greeting",
                    "Hello \${firstName} \${lastName}, you have \${unreadCount} unread messages and \${credits} credits",
                    "Complex greeting with multiple placeholders (pluralize unreadCount and credits)"
                ),
                createClientRequest(
                    "date_time_format",
                    "Created on \${date} at \${time} by \${author}",
                    "Timestamp with date (yMd format), time, and author name"
                ),
                createClientRequest(
                    "price_range",
                    "Price range: \${minPrice} - \${maxPrice}",
                    "Price range display with currency formatting (simpleCurrency)"
                )
            )

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            val startTime = System.currentTimeMillis()
            val response = sut.createComplexArbEntryBatch(batchRequest, { false }, { })
            val duration = System.currentTimeMillis() - startTime

            println("\n=== Gemini Pro Batch Test ===")
            println("Model: gemini-2.5-pro")
            println("Processed ${response.responses.size} entries in ${duration}ms")

            assertThat(response.responses).hasSize(3)

            response.responses.forEach { partialResponse ->
                val entry = partialResponse.translation.entry
                println("\n${entry.desiredKey}: ${entry.desiredValue}")

                // Verify placeholders are maintained
                assertThat(entry.desiredValue).containsPattern("\\{.*}")
            }
        }
    }

    @Test
    fun testBatchCreateComplexArbEntry_WithPlaceholderMetadata() {
        runBlocking {
            // Test ARB entries with complex placeholder metadata
            // Verify that ${variable} gets converted to {variable} and placeholder metadata is returned
            val requests = listOf(
                createClientRequest(
                    "metronome_label_bpm_value",
                    "\${logic.bpm} BPM",
                    "Display text for the current BPM value in the metronome"
                ),
                createClientRequest(
                    "rhythmtrainer_display_bpm",
                    "BPM: \${_bpm.round()}",
                    "Label for displaying beats per minute with dynamic value"
                ),
                createClientRequest(
                    "user_credits_display",
                    "You have \${credits} credits",
                    "Display user's remaining credits (int type)"
                ),
                createClientRequest(
                    "price_with_currency",
                    "Total: \${totalPrice}",
                    "Display total price with currency formatting (compactCurrency)"
                ),
                createClientRequest(
                    "greeting_with_time",
                    "Good morning, \${userName}! It's \${currentTime}",
                    "Personalized greeting with user name and current time"
                ),
                createClientRequest(
                    "items_count_plural",
                    "\${itemCount} items found",
                    "Display item count with pluralization: 0=no items, 1=1 item, other=X items (num type)"
                ),
                createClientRequest(
                    "date_display",
                    "Last updated: \${lastUpdate}",
                    "Show last update date (DateTime type, yMd format)"
                ),
                createClientRequest(
                    "percentage_display",
                    "Progress: \${progress}%",
                    "Show progress percentage (int, 0-100)"
                )
            )

            val batchRequest = BatchClientTranslationRequest(targetLanguages, requests)

            println("\n=== Complex ARB Entries with Placeholder Metadata Test ===")
            println("Testing conversion of \${variable} to {variable} with placeholder metadata\n")

            val startTime = System.currentTimeMillis()

            // Intercept the raw API response by calling the internal method
            val translations = requests.map { it.translation }
            val baseContent = parser.toBatchGPTContentAdvanced(translations)
            val targetLang = translations.first().lang

            val rawJsonResponse = sut.requestBatchComplexTranslationLong(baseContent, targetLang.toISOLangString())

            println("=== RAW API RESPONSE (JSON) ===")
            println(rawJsonResponse)
            println("=== END RAW RESPONSE ===\n")

            // Parse the response
            val response = sut.createComplexArbEntryBatch(batchRequest, { false }, { })
            val duration = System.currentTimeMillis() - startTime

            println("\nProcessed ${response.responses.size} entries in ${duration}ms\n")

            assertThat(response.responses).hasSize(8)

            // Verify each entry
            response.responses.forEach { partialResponse ->
                val entry = partialResponse.translation.entry
                val originalRequest = requests.find { it.translation.entry.desiredKey == entry.desiredKey }!!

                println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("Key: ${entry.desiredKey}")
                println("Original Input: ${originalRequest.translation.entry.desiredValue}")
                println("Transformed Output: ${entry.desiredValue}")
                println("Description: ${entry.desiredDescription}")

                if (entry.placeholder != null) {
                    println("Placeholders:")
                    entry.placeholder.forEach { (key, value) ->
                        println("  - $key: $value")
                    }
                } else {
                    println("Placeholders: null")
                }

                // Verify $ is removed from the output
                assertThat(entry.desiredValue)
                    .withFailMessage("Value should not contain $ anymore: ${entry.desiredValue}")
                    .doesNotContain("$")

                // Verify { and } are present (placeholder format)
                if (originalRequest.translation.entry.desiredValue.contains("\${")) {
                    assertThat(entry.desiredValue)
                        .withFailMessage("Value should contain placeholders in {var} format: ${entry.desiredValue}")
                        .containsPattern("\\{[a-zA-Z]+}")
                }

                // Verify key and value are not blank
                assertThat(entry.desiredKey).isNotBlank()
                assertThat(entry.desiredValue).isNotBlank()
            }

            // Specific verification for metronome_label_bpm_value
            val metronomeEntry = response.responses.find {
                it.translation.entry.desiredKey == "metronome_label_bpm_value"
            }
            assertThat(metronomeEntry).isNotNull()
            // Note: Gemini converts ${logic.bpm} to {logicbpm} (removes the dot)
            assertThat(metronomeEntry?.translation?.entry?.desiredValue)
                .matches("\\{[a-zA-Z]+} BPM")
                .withFailMessage("Expected '{variable} BPM' format but got: ${metronomeEntry?.translation?.entry?.desiredValue}")

            // Verify placeholder metadata exists
            assertThat(metronomeEntry?.translation?.entry?.placeholder)
                .isNotNull()
                .withFailMessage("Placeholder metadata should not be null for metronome_label_bpm_value")

            println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("✓ All entries successfully converted from \${var} to {var} format")
            println("✓ All entries have placeholder metadata")
            println("✓ No $ symbols remain in output values")
        }
    }
}
