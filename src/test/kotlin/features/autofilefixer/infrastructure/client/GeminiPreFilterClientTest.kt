package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.client

import com.google.genai.Client
import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.DispatcherConfiguration
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.util.concurrent.Executors


class GeminiPreFilterClientTest {
    @Mock
    private lateinit var llmConfigProvider: LLMConfigProvider

    @Mock
    private lateinit var userSettingsRepository: UserSettingsRepository

    @Mock
    private lateinit var dispatcherConfiguration: DispatcherConfiguration

    private lateinit var sut: GeminiPreFilterClient

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
                "gemini-2.5-flash",
                true,
                parallelism
            )
        )

        val key = System.getenv("gemini_api_key")
        given(llmConfigProvider.getInstanceGemini()).willReturn(
            Client.builder()
                .apiKey(key)
                .build()
        )

        sut = GeminiPreFilterClient(
            llmConfigProvider,
            userSettingsRepository,
            dispatcherConfiguration
        )
    }

    @Test
    fun testShortPreFilter_WithUIText() {
        runBlocking {
            // Given - short list with clear UI text
            val literals = listOf(
                createLiteral("1", "Login", "ElevatedButton(onPressed: () {}, child: Text('Login'))"),
                createLiteral("2", "Cancel", "TextButton(child: Text('Cancel'))"),
                createLiteral("3", "Hello World", "Text('Hello World', style: TextStyle(fontSize: 16))")
            )

            val request = PreFilterRequest(literals)

            // When
            val response = sut.preFilter(request) { current, total ->
                println("Processing batch $current of $total")
            }

            // Then
            assertThat(response.results).hasSize(3)

            // All UI texts should be marked for translation
            val result1 = response.results.find { it.id == "1" }
            assertThat(result1).isNotNull
            assertThat(result1?.shouldTranslate).isTrue()

            val result2 = response.results.find { it.id == "2" }
            assertThat(result2).isNotNull
            assertThat(result2?.shouldTranslate).isTrue()

            val result3 = response.results.find { it.id == "3" }
            assertThat(result3).isNotNull
            assertThat(result3?.shouldTranslate).isTrue()

            println("Short test results:")
            response.results.forEach {
                println("ID: ${it.id}, shouldTranslate: ${it.shouldTranslate}, reason: ${it.reason}")
            }
        }
    }

    @Test
    fun testShortPreFilter_WithTechnicalStrings() {
        runBlocking {
            // Given - short list with technical strings
            val literals = listOf(
                createLiteral("1", "[]", "var x = '[]'"),
                createLiteral("2", "debug", "print('debug')"),
                createLiteral("3", "config/path", "storeValue('config/path', value)"),
                createLiteral("4", "/", "const separator = '/'")
            )

            val request = PreFilterRequest(literals)

            // When
            val response = sut.preFilter(request) { current, total ->
                println("Processing batch $current of $total")
            }

            // Then
            assertThat(response.results).hasSize(4)

            // All technical strings should NOT be marked for translation
            response.results.forEach { result ->
                assertThat(result.shouldTranslate).isFalse()
                println("ID: ${result.id}, shouldTranslate: ${result.shouldTranslate}, reason: ${result.reason}")
            }
        }
    }

    @Test
    fun testShortPreFilter_MixedContent() {
        runBlocking {
            // Given - mixed UI and technical strings
            val literals = listOf(
                createLiteral("1", "Save", "ElevatedButton(child: Text('Save'))"),
                createLiteral("2", "debug_mode", "const DEBUG_MODE = 'debug_mode'"),
                createLiteral("3", "Welcome back!", "Text('Welcome back!')"),
                createLiteral("4", "api/v1/users", "final endpoint = 'api/v1/users'"),
                createLiteral("5", "No information required", "var message = 'No information required'")
            )

            val request = PreFilterRequest(literals)

            // When
            val response = sut.preFilter(request) { current, total ->
                println("Processing batch $current of $total")
            }

            // Then
            assertThat(response.results).hasSize(5)

            // Check expected results
            val result1 = response.results.find { it.id == "1" }
            assertThat(result1?.shouldTranslate).isTrue() // "Save" button

            val result2 = response.results.find { it.id == "2" }
            assertThat(result2?.shouldTranslate).isFalse() // debug_mode constant

            val result3 = response.results.find { it.id == "3" }
            assertThat(result3?.shouldTranslate).isTrue() // Welcome message

            val result4 = response.results.find { it.id == "4" }
            assertThat(result4?.shouldTranslate).isFalse() // API endpoint

            val result5 = response.results.find { it.id == "5" }
            assertThat(result5?.shouldTranslate).isTrue() // User message

            println("Mixed content test results:")
            response.results.forEach {
                println("ID: ${it.id}, literal: '${literals.find { l -> l.id == it.id }?.literalText}', shouldTranslate: ${it.shouldTranslate}")
            }
        }
    }

    @Test
    fun testLongerPreFilter_MultipleBatches() {
        runBlocking {
            // Given - longer list that will trigger batching (simulate ~100 literals)
            val literals = mutableListOf<PreFilterLiteral>()

            // Add UI text literals
            repeat(30) { i ->
                literals.add(createLiteral("ui_$i", "Button ${i + 1}", "ElevatedButton(child: Text('Button ${i + 1}'))"))
            }

            // Add technical literals
            repeat(30) { i ->
                literals.add(createLiteral("tech_$i", "config_key_$i", "const KEY_$i = 'config_key_$i'"))
            }

            // Add mixed content
            literals.add(createLiteral("mix_1", "Error occurred", "showDialog(title: 'Error occurred')"))
            literals.add(createLiteral("mix_2", "/api/error", "final errorEndpoint = '/api/error'"))
            literals.add(createLiteral("mix_3", "Please try again", "Text('Please try again')"))
            literals.add(createLiteral("mix_4", "[]", "var emptyList = '[]'"))
            literals.add(createLiteral("mix_5", "Settings", "AppBar(title: Text('Settings'))"))
            literals.add(createLiteral("mix_6", "debug_log", "print('debug_log')"))

            val request = PreFilterRequest(literals)

            var batchCount = 0
            var totalBatches = 0

            // When
            val response = sut.preFilter(request) { current, total ->
                batchCount = current
                totalBatches = total
                println("Processing batch $current of $total")
            }

            // Then
            assertThat(response.results).hasSize(66)
            println("Processed ${response.results.size} literals in $totalBatches batches")

            // Check that batching occurred (with parallelism = 3, should have multiple batches)
            if (totalBatches > 1) {
                println("✓ Multiple batches were processed in parallel")
            }

            // Count UI vs technical
            val uiCount = response.results.count { it.shouldTranslate }
            val technicalCount = response.results.count { !it.shouldTranslate }

            println("Results: $uiCount should translate, $technicalCount should NOT translate")

            // Most UI texts should be marked for translation
            val uiResults = response.results.filter { it.id.startsWith("ui_") }
            val uiTranslateCount = uiResults.count { it.shouldTranslate }
            assertThat(uiTranslateCount).isGreaterThan(uiResults.size / 2) // At least half

            // Most technical strings should NOT be marked for translation
            val techResults = response.results.filter { it.id.startsWith("tech_") }
            val techNoTranslateCount = techResults.count { !it.shouldTranslate }
            assertThat(techNoTranslateCount).isGreaterThan(techResults.size / 2) // At least half

            // Check specific mixed results
            val errorMessage = response.results.find { it.id == "mix_1" }
            assertThat(errorMessage?.shouldTranslate).isTrue() // "Error occurred" should translate

            val apiPath = response.results.find { it.id == "mix_2" }
            assertThat(apiPath?.shouldTranslate).isFalse() // API path should NOT translate

            val tryAgain = response.results.find { it.id == "mix_3" }
            assertThat(tryAgain?.shouldTranslate).isTrue() // "Please try again" should translate

            val emptyBrackets = response.results.find { it.id == "mix_4" }
            assertThat(emptyBrackets?.shouldTranslate).isFalse() // "[]" should NOT translate

            val settings = response.results.find { it.id == "mix_5" }
            assertThat(settings?.shouldTranslate).isTrue() // "Settings" should translate

            val debugLog = response.results.find { it.id == "mix_6" }
            assertThat(debugLog?.shouldTranslate).isFalse() // "debug_log" should NOT translate
        }
    }

    @Test
    fun testVeryLongPreFilter_ParallelBatching() {
        runBlocking {
            // Given - very long list to test parallel batch processing
            val literals = mutableListOf<PreFilterLiteral>()

            // Create enough literals to trigger multiple parallel batches
            repeat(150) { i ->
                val isUiText = i % 3 == 0 // Every 3rd is UI text
                if (isUiText) {
                    literals.add(createLiteral("$i", "Message $i", "Text('Message $i')"))
                } else {
                    literals.add(createLiteral("$i", "config_$i", "const CONFIG_$i = 'config_$i'"))
                }
            }

            val request = PreFilterRequest(literals)

            var maxBatch = 0
            val startTime = System.currentTimeMillis()

            // When
            val response = sut.preFilter(request) { current, total ->
                maxBatch = total
                println("Processing batch $current of $total")
            }

            val duration = System.currentTimeMillis() - startTime

            // Then
            assertThat(response.results).hasSize(150)
            println("Processed 150 literals in ${duration}ms across $maxBatch batches")
            println("Parallelism helped process multiple batches concurrently")

            // Verify all IDs are present
            val ids = response.results.map { it.id }.sorted()
            assertThat(ids).hasSize(150)

            println("✓ All 150 literals were processed successfully")
        }
    }

    @Test
    fun testCombinedContext_20Sections() {
        runBlocking {
            // Create a mixed list of ~100 literals with varying complexity
            // Mix simple, medium, and highly complex Flutter structures to test batching
            val literals = mutableListOf<PreFilterLiteral>()
            var idCounter = 1

            // Helper function to add literal with auto-incrementing ID
            fun addLiteral(text: String, context: String) {
                literals.add(createLiteral(idCounter.toString(), text, context))
                idCounter++
            }

            // Add simple literals first
            addLiteral("OK", "TextButton(child: Text('OK'))")
            addLiteral("Cancel", "TextButton(child: Text('Cancel'))")
            addLiteral("debug_key", "const DEBUG_KEY = 'debug_key'")

            // Add medium complexity widget
            addLiteral("Settings", """
                AppBar(
                  title: Text('Settings'),
                  actions: [
                    IconButton(icon: Icon(Icons.search), onPressed: () {}),
                  ],
                )
            """.trimIndent())

            addLiteral("Error", "showDialog(title: 'Error')")
            addLiteral("config/path", "storeValue('config/path', value)")

            // Complex nested widget #1
            addLiteral("We're here now", """
                Container(
                  padding: EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Card(
                        elevation: 4.0,
                        child: Padding(
                          padding: EdgeInsets.all(12.0),
                          child: Column(
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Icon(Icons.shopping_cart),
                                  Text('Shopping Cart', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                                  Badge(child: Icon(Icons.notifications)),
                                ],
                              ),
                              SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text('Product Name', style: TextStyle(fontSize: 16)),
                                        Text('Description here', style: TextStyle(color: Colors.grey)),
                                        Row(
                                          children: [
                                            ElevatedButton(
                                              onPressed: () {},
                                              child: Text('Add to Cart'),
                                            ),
                                            SizedBox(width: 8),
                                            debugPrint("We're here now"),
                                            OutlinedButton(
                                              onPressed: () {},
                                              child: Text('View Details'),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                )
            """.trimIndent())

            // Add more simple ones
            addLiteral("Save", "ElevatedButton(child: Text('Save'))")
            addLiteral("api_key", "const API_KEY = 'api_key'")
            addLiteral("Welcome", "Text('Welcome')")

            // Medium complexity widget
            addLiteral("Delete Item", """
                AlertDialog(
                  title: Text('Delete Item'),
                  content: Text('Are you sure you want to delete this item?'),
                  actions: [
                    TextButton(onPressed: () {}, child: Text('Cancel')),
                    ElevatedButton(onPressed: () {}, child: Text('Delete')),
                  ],
                )
            """.trimIndent())

            addLiteral("/", "const separator = '/'")
            addLiteral("Next", "IconButton(icon: Icon(Icons.arrow_forward), onPressed: () {})")

            // Complex nested widget #2
            addLiteral("User Profile Settings", """
                ListView.builder(
                  itemCount: items.length,
                  itemBuilder: (context, index) {
                    return Card(
                      margin: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                      child: ExpansionTile(
                        leading: CircleAvatar(child: Icon(Icons.person)),
                        title: Text('User Profile Settings'),
                        subtitle: Text('Configure your preferences'),
                        children: [
                          ListTile(
                            leading: Icon(Icons.language),
                            title: Text('Language'),
                            subtitle: Text('Choose your preferred language'),
                            trailing: Icon(Icons.arrow_forward),
                            onTap: () {},
                          ),
                          Divider(),
                          ListTile(
                            leading: Icon(Icons.notifications),
                            title: Text('Notifications'),
                            subtitle: Text('Manage notification settings'),
                            trailing: Switch(value: true, onChanged: (val) {}),
                          ),
                          Padding(
                            padding: EdgeInsets.all(16),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                TextButton(onPressed: () {}, child: Text('Cancel')),
                                SizedBox(width: 8),
                                ElevatedButton(onPressed: () {}, child: Text('Save Changes')),
                              ],
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                )
            """.trimIndent())

            addLiteral("Loading", "CircularProgressIndicator()")
            addLiteral("[]", "var x = '[]'")

            // Add more medium complexity widgets
            repeat(15) { i ->
                addLiteral("Item ${i + 1}", """
                    Card(
                      child: ListTile(
                        leading: Icon(Icons.star),
                        title: Text('Item ${i + 1}'),
                        subtitle: Text('Description for item ${i + 1}'),
                        trailing: IconButton(icon: Icon(Icons.more_vert), onPressed: () {}),
                      ),
                    )
                """.trimIndent())
            }

            // Add simple technical strings
            repeat(20) { i ->
                addLiteral("key_$i", "const KEY_$i = 'key_$i'")
            }

            // Complex nested widget #3
            addLiteral("Welcome Back!", """
                Stack(
                  children: [
                    Container(
                      height: 200,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(colors: [Colors.blue, Colors.purple]),
                      ),
                    ),
                    Positioned(
                      top: 40,
                      left: 20,
                      right: 20,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Welcome Back!',
                            style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: Colors.white)),
                          SizedBox(height: 8),
                          Text('Ready to continue your journey?',
                            style: TextStyle(fontSize: 16, color: Colors.white70)),
                          SizedBox(height: 20),
                          Row(
                            children: [
                              Expanded(
                                child: ElevatedButton.icon(
                                  onPressed: () {},
                                  icon: Icon(Icons.play_arrow),
                                  label: Text('Continue'),
                                  style: ElevatedButton.styleFrom(padding: EdgeInsets.all(16)),
                                ),
                              ),
                              SizedBox(width: 12),
                              Expanded(
                                child: OutlinedButton.icon(
                                  onPressed: () {},
                                  icon: Icon(Icons.info),
                                  label: Text('Learn More'),
                                  style: OutlinedButton.styleFrom(
                                    padding: EdgeInsets.all(16),
                                    side: BorderSide(color: Colors.white),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                )
            """.trimIndent())

            // Add more simple UI texts
            repeat(15) { i ->
                addLiteral("Button ${i + 1}", "ElevatedButton(child: Text('Button ${i + 1}'))")
            }

            // Complex nested widget #4
            addLiteral("Category Name", """
                GridView.builder(
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    crossAxisSpacing: 12,
                    mainAxisSpacing: 12,
                    childAspectRatio: 0.75,
                  ),
                  padding: EdgeInsets.all(16),
                  itemCount: categories.length,
                  itemBuilder: (context, index) {
                    return Card(
                      elevation: 3,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      child: InkWell(
                        onTap: () {},
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(
                              padding: EdgeInsets.all(20),
                              decoration: BoxDecoration(
                                color: Colors.blue.shade50,
                                shape: BoxShape.circle,
                              ),
                              child: Icon(Icons.category, size: 40, color: Colors.blue),
                            ),
                            SizedBox(height: 12),
                            Text('Category Name',
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                              textAlign: TextAlign.center),
                            SizedBox(height: 4),
                            Text('25 items available',
                              style: TextStyle(fontSize: 12, color: Colors.grey)),
                            SizedBox(height: 8),
                            Chip(
                              label: Text('New'),
                              backgroundColor: Colors.green.shade100,
                              labelStyle: TextStyle(fontSize: 10, color: Colors.green.shade900),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                )
            """.trimIndent())

            // Add more medium complexity
            repeat(10) { i ->
                addLiteral("Dialog ${i + 1}", """
                    showDialog(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: Text('Dialog ${i + 1}'),
                        content: Text('This is dialog number ${i + 1}'),
                        actions: [TextButton(child: Text('Close'), onPressed: () {})],
                      ),
                    )
                """.trimIndent())
            }

            // Complex nested widget #5
            addLiteral("Create Your Account", """
                Form(
                  key: _formKey,
                  child: SingleChildScrollView(
                    padding: EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text('Create Your Account',
                          style: Theme.of(context).textTheme.headlineMedium,
                          textAlign: TextAlign.center),
                        SizedBox(height: 24),
                        Card(
                          elevation: 2,
                          child: Padding(
                            padding: EdgeInsets.all(16),
                            child: Column(
                              children: [
                                TextFormField(
                                  decoration: InputDecoration(
                                    labelText: 'Full Name',
                                    hintText: 'Enter your full name',
                                    prefixIcon: Icon(Icons.person),
                                    border: OutlineInputBorder(),
                                  ),
                                  validator: (value) {
                                    if (value == null || value.isEmpty) {
                                      return 'Please enter your name';
                                    }
                                    return null;
                                  },
                                ),
                                SizedBox(height: 16),
                                TextFormField(
                                  decoration: InputDecoration(
                                    labelText: 'Email Address',
                                    hintText: 'your.email@example.com',
                                    prefixIcon: Icon(Icons.email),
                                    border: OutlineInputBorder(),
                                  ),
                                  validator: (value) {
                                    if (value == null || value.isEmpty) {
                                      return 'Email is required';
                                    }
                                    if (!value.contains('@')) {
                                      return 'Please enter a valid email';
                                    }
                                    return null;
                                  },
                                ),
                                SizedBox(height: 16),
                                TextFormField(
                                  obscureText: true,
                                  decoration: InputDecoration(
                                    labelText: 'Password',
                                    hintText: 'Choose a strong password',
                                    prefixIcon: Icon(Icons.lock),
                                    suffixIcon: IconButton(
                                      icon: Icon(Icons.visibility),
                                      onPressed: () {},
                                    ),
                                    border: OutlineInputBorder(),
                                  ),
                                  validator: (value) {
                                    if (value == null || value.length < 8) {
                                      return 'Password must be at least 8 characters';
                                    }
                                    return null;
                                  },
                                ),
                                SizedBox(height: 24),
                                Row(
                                  children: [
                                    Checkbox(value: false, onChanged: (val) {}),
                                    Expanded(
                                      child: Text('I agree to the Terms and Conditions',
                                        style: TextStyle(fontSize: 14)),
                                    ),
                                  ],
                                ),
                                SizedBox(height: 24),
                                ElevatedButton(
                                  onPressed: () {},
                                  style: ElevatedButton.styleFrom(
                                    padding: EdgeInsets.symmetric(vertical: 16),
                                  ),
                                  child: Text('Sign Up', style: TextStyle(fontSize: 18)),
                                ),
                                SizedBox(height: 12),
                                TextButton(
                                  onPressed: () {},
                                  child: Text('Already have an account? Log in'),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                )
            """.trimIndent())

            // Add final batch of simple literals
            repeat(10) { i ->
                addLiteral("Message ${i + 1}", "Text('Message ${i + 1}')")
            }

            val request = PreFilterRequest(literals)

            var totalBatches = 0
            val startTime = System.currentTimeMillis()

            // When
            val response = sut.preFilter(request) { current, total ->
                totalBatches = total
                println("Processing batch $current of $total")
            }

            val duration = System.currentTimeMillis() - startTime

            // Then
            val expectedSize = idCounter - 1 // Since we start at 1 and increment after each add
            assertThat(response.results).hasSize(expectedSize)
            println("\n=== Mixed Complexity Context Test Results ===")
            println("Processed $expectedSize literals in ${duration}ms across $totalBatches batches")
            println("Average time per literal: ${duration / expectedSize}ms")

            // Verify some key results
            val result1 = response.results.find { it.id == "1" }
            println("ID 1 (OK): shouldTranslate=${result1?.shouldTranslate}")

            val result3 = response.results.find { it.id == "3" }
            println("ID 3 (debug_key): shouldTranslate=${result3?.shouldTranslate}")
            assertThat(result3?.shouldTranslate).isFalse()

            val result7 = response.results.find { it.id == "7" }
            println("ID 7 (We're here now - complex): shouldTranslate=${result7?.shouldTranslate}")
            assertThat(result7?.shouldTranslate).isFalse() // It's in debugPrint

            // Count results by type
            val shouldTranslate = response.results.count { it.shouldTranslate }
            val shouldNotTranslate = response.results.count { !it.shouldTranslate }
            println("\nResults Summary:")
            println("  Should translate: $shouldTranslate")
            println("  Should NOT translate: $shouldNotTranslate")
            println("  Total batches processed: $totalBatches")

            // Verify that batching occurred
            assertThat(totalBatches).isGreaterThan(1)
            println("\n✓ Successfully processed mixed complexity contexts with multiple batches")
        }
    }

    private fun createLiteral(id: String, text: String, context: String): PreFilterLiteral {
        return PreFilterLiteral(
            id = id,
            literalText = text,
            context = context,
            psiElement = mock(PsiElement::class.java)
        )
    }
}
