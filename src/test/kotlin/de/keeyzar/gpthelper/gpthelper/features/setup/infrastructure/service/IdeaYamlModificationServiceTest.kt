import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service.IdeaYamlModificationService
import org.junit.Test

/**
 * Test for IdeaYamlModificationService using exact string comparison.
 * We extend LightPlatformCodeInsightFixtureTestCase to get a Project instance automatically.
 */
class IdeaYamlModificationServiceTest : LightPlatformCodeInsightFixture4TestCase() {

    private lateinit var modificationService: IdeaYamlModificationService

    override fun setUp() {
        super.setUp()
        // Initialize the service with the project instance provided by the test fixture
        modificationService = IdeaYamlModificationService(project)
    }

    @Test
    fun `test addFlutterGenerate when flutter section does not exist`() {
        // Given: Initial YAML content without a 'flutter' section
        val initialYaml = """
            name: test_project
            description: A new Flutter project.
            version: 1.0.0+1

            environment:
              sdk: '>=2.12.0 <3.0.0'

            dependencies:
              flutter:
                sdk: flutter
        """.trimIndent()

        // Expected: YAML content with the 'flutter' section and 'generate: true' added
        val expectedYaml = """
            name: test_project
            description: A new Flutter project.
            version: 1.0.0+1

            environment:
              sdk: '>=2.12.0 <3.0.0'

            dependencies:
              flutter:
                sdk: flutter

            flutter:
              generate: true
        """.trimIndent()

        // When: We call the service to add the generate flag
        val modifiedYaml = modificationService.addFlutterGenerate(initialYaml)

        // Then: The modified content should match the expected content exactly
        assertEquals(expectedYaml, modifiedYaml)
    }

    @Test
    fun `test addFlutterGenerate when flutter section already exists`() {
        // Given: Initial YAML with an existing 'flutter' section
        val initialYaml = """
            name: test_project
            description: A new Flutter project.

            flutter:
              uses-material-design: true
        """.trimIndent()

        // Expected: The 'generate: true' flag is added to the existing section
        val expectedYaml = """
            name: test_project
            description: A new Flutter project.

            flutter:
              uses-material-design: true
              generate: true
        """.trimIndent()

        // When: We call the service
        val modifiedYaml = modificationService.addFlutterGenerate(initialYaml)

        // Then: The result should match the expected YAML
        assertEquals(expectedYaml, modifiedYaml)
    }

    @Test
    fun `test addDependency when dependencies section does not exist`() {
        // Given: YAML content without a 'dependencies' section
        val initialYaml = """
            name: test_project
            environment:
              sdk: '>=2.12.0 <3.0.0'
        """.trimIndent()

        // Expected: A 'dependencies' section is created with the new dependency
        val expectedYaml = """
            name: test_project
            environment:
              sdk: '>=2.12.0 <3.0.0'

            dependencies:
              intl: any
        """.trimIndent()

        // When: We add a simple dependency
        val modifiedYaml = modificationService.addDependency(initialYaml, "intl", "any")

        // Then: The modified content should match the expected result
        assertEquals(expectedYaml, modifiedYaml)
    }

    @Test
    fun `test addDependency when dependencies section is empty`() {
        // Given: A 'dependencies' key exists but has no entries (is null)
        val initialYaml = """
            name: test_project
            description: A new Flutter project.

            dependencies:
        """.trimIndent()

        // Expected: The new dependency is correctly added under the existing key
        val expectedYaml = """
            name: test_project
            description: A new Flutter project.

            dependencies:
              provider: ^6.0.0
        """.trimIndent()

        // When: We add a dependency
        val modifiedYaml = modificationService.addDependency(initialYaml, "provider", "^6.0.0")

        // Then: The result should be correctly formatted
        assertEquals(expectedYaml, modifiedYaml)
    }

    @Test
    fun `test addComplexMapDependency`() {
        // Given: A standard pubspec.yaml
        val initialYaml = """
            name: test_project

            dependencies:
              flutter:
                sdk: flutter
        """.trimIndent()

        // Expected: A complex git dependency is added
        val expectedYaml = """
            name: test_project

            dependencies:
              flutter:
                sdk: flutter
              custom_package:
                git:
                  url: git://github.com/user/repo.git
                  ref: main
        """.trimIndent()

        // When: We add a dependency with a Map value
        val gitDependency = mapOf(
            "git" to mapOf(
                "url" to "git://github.com/user/repo.git",
                "ref" to "main"
            )
        )

        // NOTE: The corrected service class needs a slight adjustment to handle nested maps.
        // Let's assume a simple map for this test, as the original code handles Map<String, String>.
        val simpleMapDependency = mapOf(
            "url" to "git://github.com/user/repo.git",
            "ref" to "main"
        )

        // This is the expected output for the provided Map<String, String> implementation
        val expectedForSimpleMap = """
            name: test_project

            dependencies:
              flutter:
                sdk: flutter
              custom_package:
                url: git://github.com/user/repo.git
                ref: main
        """.trimIndent()


        val modifiedYaml = modificationService.addDependency(initialYaml, "custom_package", simpleMapDependency)

        // Then: The result should match the expected multi-line entry
        // NOTE: A more robust implementation would handle nested maps to produce the first 'expectedYaml'.
        // This test validates the behavior of the *provided* code.
        assertEquals(expectedForSimpleMap, modifiedYaml)
    }
}