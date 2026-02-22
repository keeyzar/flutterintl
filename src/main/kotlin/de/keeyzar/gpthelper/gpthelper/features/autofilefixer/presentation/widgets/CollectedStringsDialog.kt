package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.application.ReadAction
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
import de.keeyzar.gpthelper.gpthelper.features.psiutils.SmartPsiElementWrapper
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
    private val stringLiteralsWithSelection: Map<SmartPsiElementWrapper<PsiElement>, Boolean>,
    /**
     * Set of reviewed-string IDs that were previously skipped by the user.
     * These will be shown in a separate "Previously Skipped" section, unchecked by default.
     */
    private val previouslySkippedIds: Set<String> = emptySet(),
    /**
     * Function to compute the reviewed-string ID for a PsiElement.
     * If null, no splitting into sections will occur.
     */
    private val idGenerator: ((PsiElement) -> String)? = null,
) : DialogWrapper(project) {

    private lateinit var contextTextArea: JBTextArea
    private lateinit var tree: CheckboxTree
    private val rootNode = CheckedTreeNode(project.name)
    private val newStringsNode = CheckedTreeNode("New Strings")
    private val previouslySkippedNode = CheckedTreeNode("Previously Skipped")

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
            if (previouslySkippedIds.isNotEmpty()) {
                add(Box.createHorizontalStrut(10))
                val resetSkippedButton = JButton("Check All Skipped")
                resetSkippedButton.toolTipText = "Re-check all previously skipped strings so you can review them again"
                resetSkippedButton.addActionListener {
                    setAllChecked(previouslySkippedNode, true)
                    (tree.model as DefaultTreeModel).nodeChanged(previouslySkippedNode)
                    tree.repaint()
                }
                add(resetSkippedButton)
            }
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

    private fun buildTree(stringLiteralsWithSelection: Map<SmartPsiElementWrapper<PsiElement>, Boolean>) {
        // Resolve all pointers to current PSI elements, filtering out invalidated ones
        // SmartPointer.getElement() requires read access
        val validElements = ReadAction.compute<Map<PsiElement, Boolean>, Throwable> {
            stringLiteralsWithSelection.mapNotNull { (wrapper, selected) ->
                wrapper.element?.let { it to selected }
            }.toMap()
        }

        // Split elements into new vs previously skipped
        val hasSections = previouslySkippedIds.isNotEmpty() && idGenerator != null
        val newElements = mutableMapOf<PsiElement, Boolean>()
        val skippedElements = mutableMapOf<PsiElement, Boolean>()

        if (hasSections) {
            ReadAction.run<Throwable> {
                for ((element, selected) in validElements) {
                    val id = idGenerator!!.invoke(element)
                    if (id in previouslySkippedIds) {
                        // Previously skipped: unchecked by default
                        skippedElements[element] = false
                    } else {
                        newElements[element] = selected
                    }
                }
            }
        } else {
            newElements.putAll(validElements)
        }

        if (hasSections && skippedElements.isNotEmpty()) {
            // Build two sections under root
            rootNode.add(newStringsNode)
            buildSubTree(newStringsNode, newElements)
            rootNode.add(previouslySkippedNode)
            previouslySkippedNode.isChecked = false
            buildSubTree(previouslySkippedNode, skippedElements)
        } else {
            // No sections needed - build flat like before
            buildSubTree(rootNode, newElements)
        }
    }

    private fun buildSubTree(parentNode: CheckedTreeNode, elements: Map<PsiElement, Boolean>) {
        val fileToLiteralsMap = elements.keys.groupBy { it.containingFile }
        val nodeMap = mutableMapOf<String, CheckedTreeNode>()

        for ((psiFile, literals) in fileToLiteralsMap) {
            if (psiFile == null) continue

            // Get relative path from project base
            val relativePath = psiFile.virtualFile.path.substringAfter(project.basePath!! + "/")
            val pathComponents = relativePath.split('/')

            var currentParentNode = parentNode
            var currentPath = parentNode.userObject.toString() // scope node map per section

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
                literalNode.isChecked = elements[literal] ?: true // Select based on map, default to true
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
        val contextFinder = FlutterArbTranslationInitializer.create(element.project).literalInContextFinder
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
     * Returns all string literals that were NOT selected by the user.
     * These are candidates for persisting as "previously skipped".
     */
    fun getUnselectedLiterals(): List<PsiElement> {
        val unselectedLiterals = mutableListOf<PsiElement>()
        collectUnselectedLiterals(rootNode, unselectedLiterals)
        return unselectedLiterals
    }

    private fun collectUnselectedLiterals(node: CheckedTreeNode, unselected: MutableList<PsiElement>) {
        if (!node.isChecked && node.userObject is PsiElement && node.userObject !is PsiFile) {
            unselected.add(node.userObject as PsiElement)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) {
                collectUnselectedLiterals(child, unselected)
            }
        }
    }

    private fun setAllChecked(node: CheckedTreeNode, checked: Boolean) {
        node.isChecked = checked
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) {
                setAllChecked(child, checked)
            }
        }
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
                is String -> {
                    if (userObject == "New Strings" || userObject == "Previously Skipped") {
                        val count = countLeafNodes(node)
                        "$userObject ($count)"
                    } else {
                        userObject
                    }
                }
                else -> "Project" // Root node
            }
            this.textRenderer.append(text)
            if (userObject is String && userObject == "Previously Skipped") {
                this.textRenderer.append(" — unchecked by default", com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }

        private fun countLeafNodes(node: CheckedTreeNode): Int {
            var count = 0
            for (i in 0 until node.childCount) {
                val child = node.getChildAt(i)
                if (child is CheckedTreeNode) {
                    if (child.userObject is PsiElement && child.userObject !is PsiFile) {
                        count++
                    } else {
                        count += countLeafNodes(child)
                    }
                }
            }
            return count
        }
    }
}