package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.util.ui.tree.TreeUtil
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class CollectedStringsDialog(
    private val project: Project,
    private val stringLiteralsWithSelection: Map<PsiElement, Boolean>
) : DialogWrapper(project) {

    private lateinit var contextTextArea: JBTextArea
    private lateinit var tree: CheckboxTree
    private val rootNode = CheckedTreeNode(project.name)

    init {
        title = "Select Strings to Localize"
        setOKButtonText("Localize Selected")
        buildTree(stringLiteralsWithSelection)
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val leftPanel = createTreePanel()
        val rightPanel = createContextPanel()

        tree.addTreeSelectionListener {
            val selectedNode = tree.lastSelectedPathComponent as? CheckedTreeNode
            val userObject = selectedNode?.userObject
            if (userObject is PsiElement && userObject.isValid) { // Check if it's a literal
                updateContextView(userObject)
            } else {
                contextTextArea.text = ""
            }
        }

        // Expand all nodes by default
        TreeUtil.expandAll(tree)
        // Select the first file node by default
        selectFirstFileNode()

        return JBSplitter(false, 0.4f).apply {
            firstComponent = leftPanel
            secondComponent = rightPanel
            preferredSize = Dimension(850, 600)
        }
    }

    private fun createTreePanel(): JComponent {
        tree = CheckboxTree(MyTreeCellRenderer(), rootNode)
        tree.model = DefaultTreeModel(rootNode)

        val expandAllButton = JButton("Expand All")
        expandAllButton.addActionListener { TreeUtil.expandAll(tree) }

        val collapseAllButton = JButton("Collapse All")
        collapseAllButton.addActionListener { TreeUtil.collapseAll(tree, 1) }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(expandAllButton)
            add(collapseAllButton)
            add(Box.createHorizontalGlue())
        }

        return JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.NORTH)
            add(JBScrollPane(tree), BorderLayout.CENTER)
        }
    }

    private fun createContextPanel(): DialogPanel {
        return panel {
            group("Context") {
                row {
                    textArea()
                        .align(Align.FILL)
                        .rows(20)
                        .also { contextTextArea = it.component }
                        .applyToComponent {
                            isEditable = false
                            isFocusable = false
                            lineWrap = true
                            wrapStyleWord = true
                        }
                }.resizableRow()
            }
        }
    }

    private fun buildTree(stringLiteralsWithSelection: Map<PsiElement, Boolean>) {
        val fileToLiteralsMap = stringLiteralsWithSelection.keys.groupBy { it.containingFile }
        val nodeMap = mutableMapOf<String, CheckedTreeNode>()

        for ((psiFile, literals) in fileToLiteralsMap) {
            if (psiFile == null) continue

            // Get relative path from project base
            val relativePath = psiFile.virtualFile.path.substringAfter(project.basePath!! + "/")
            val pathComponents = relativePath.split('/')

            var currentParentNode = rootNode
            var currentPath = ""

            // Create directory nodes
            for (i in 0 until pathComponents.size - 1) {
                val part = pathComponents[i]
                currentPath += "/$part"
                var childNode = nodeMap[currentPath]
                if (childNode == null) {
                    childNode = CheckedTreeNode(part) // User object is the directory name string
                    nodeMap[currentPath] = childNode
                    currentParentNode.add(childNode)
                }
                currentParentNode = childNode
            }

            // Create file node
            val fileNode = CheckedTreeNode(psiFile) // User object is the PsiFile
            currentParentNode.add(fileNode)

            // Create literal nodes
            for (literal in literals) {
                val literalNode = CheckedTreeNode(literal) // User object is the PsiElement
                literalNode.isChecked = stringLiteralsWithSelection[literal] ?: true // Select based on map, default to true
                fileNode.add(literalNode)
            }
        }
    }

    private fun selectFirstFileNode() {
        val firstFileNode = TreeUtil.findNode(rootNode) {
            it.userObject is PsiFile
        }
        if (firstFileNode != null) {
            val path = TreePath((firstFileNode as CheckedTreeNode).path)
            tree.selectionPath = path
            tree.scrollPathToVisible(path)
        }
    }

    private fun updateContextView(element: PsiElement) {
        val contextFinder = FlutterArbTranslationInitializer().literalInContextFinder
        val fullContextText = contextFinder.findContext(element).text
        contextTextArea.text = fullContextText

        // Clear previous highlights
        contextTextArea.highlighter.removeAllHighlights()

        val stringToHighlight = element.text
        val startIndex = fullContextText.indexOf(stringToHighlight)

        if (startIndex != -1) {
            val endIndex = startIndex + stringToHighlight.length
            try {
                val highlightPainter: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(JBColor.yellow.darker().darker())
                contextTextArea.highlighter.addHighlight(startIndex, endIndex, highlightPainter)
            } catch (e: Exception) {
                // Log or handle the exception if highlighting fails
                println("Error highlighting text: ${e.message}")
            }
        }
    }

    fun getSelectedLiterals(): List<PsiElement> {
        val selectedLiterals = mutableListOf<PsiElement>()
        collectSelectedLiterals(rootNode, selectedLiterals)
        return selectedLiterals
    }

    private fun collectSelectedLiterals(node: CheckedTreeNode, selected: MutableList<PsiElement>) {
        if (node.isChecked && node.userObject is PsiElement && node.userObject !is PsiFile) {
            selected.add(node.userObject as PsiElement)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) {
                collectSelectedLiterals(child, selected)
            }
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tree
    }

    /**
     * Overrides the OK action to validate that at least one string is selected.
     */
    override fun doOKAction() {
        if (getSelectedLiterals().isNotEmpty()) {
            super.doOKAction()
        } else {
            Messages.showErrorDialog(
                project,
                "Please select at least one string to localize.",
                "No Strings Selected"
            )
        }
    }

    private class MyTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer(true) {
        override fun customizeRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            val userObject = node.userObject
            val text = when (userObject) {
                is PsiFile -> userObject.name
                is PsiElement -> userObject.text
                is String -> userObject
                else -> "Project" // Root node
            }
            this.textRenderer.append(text)
        }
    }
}